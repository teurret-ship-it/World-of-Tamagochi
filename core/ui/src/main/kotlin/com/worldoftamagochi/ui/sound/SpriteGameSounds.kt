package com.worldoftamagochi.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.worldoftamagochi.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

/**
 * Decodes the SND sprite once (off the main thread) and keeps every cue as a
 * static AudioTrack, so playing a cue is instant and allocation-free.
 * Until decoding finishes, cues are silently skipped.
 */
class SpriteGameSounds(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GameSounds {
    private val appContext = context.applicationContext

    @Volatile
    private var tracks: Map<Sfx, AudioTrack> = emptyMap()

    init {
        scope.launch {
            runCatching { tracks = load() }.onFailure { Log.w(TAG, "Sound sprite unavailable", it) }
        }
    }

    override fun play(sfx: Sfx) {
        val track = tracks[sfx] ?: return
        scope.launch {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                track.reloadStaticData()
                track.play()
            }
        }
    }

    fun release() {
        tracks.values.forEach { it.release() }
        tracks = emptyMap()
    }

    private fun load(): Map<Sfx, AudioTrack> {
        val pcm = decode()
        return Sfx.entries.associateWith { sfx ->
            val from = pcm.frameOffset(sfx.startMillis)
            val to = pcm.frameOffset(sfx.endMillis).coerceAtMost(pcm.samples.size)
            val slice = pcm.samples.copyOfRange(from, to)
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(pcm.sampleRate)
                        .setChannelMask(if (pcm.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(slice.size * BYTES_PER_SAMPLE)
                .build()
                .also { it.write(slice, 0, slice.size) }
        }
    }

    private class Pcm(
        val samples: ShortArray,
        val sampleRate: Int,
        val channels: Int,
    ) {
        fun frameOffset(millis: Int): Int = (millis.toLong() * sampleRate / MILLIS_PER_SECOND).toInt() * channels
    }

    private fun decode(): Pcm {
        val extractor = MediaExtractor()
        appContext.resources.openRawResourceFd(R.raw.snd_sprite).use { fd ->
            extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        }
        val format = extractor.getTrackFormat(0)
        extractor.selectTrack(0)
        val codec = MediaCodec.createDecoderByType(requireNotNull(format.getString(MediaFormat.KEY_MIME)))
        codec.configure(format, null, null, 0)
        codec.start()
        val out = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputFormat = format
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) inputDone = feedInput(codec, extractor)
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) outputFormat = codec.outputFormat
            if (index >= 0) outputDone = drainOutput(codec, index, info, out)
        }
        codec.stop()
        codec.release()
        extractor.release()
        return Pcm(
            samples = out.toByteArray().toShorts(),
            sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
        )
    }

    /** Queues one compressed sample; returns true once the stream has ended. */
    private fun feedInput(
        codec: MediaCodec,
        extractor: MediaExtractor,
    ): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val size = extractor.readSampleData(requireNotNull(codec.getInputBuffer(index)), 0)
        val ended = size < 0
        if (ended) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        } else {
            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
            extractor.advance()
        }
        return ended
    }

    /** Copies one decoded buffer out; returns true at the end of the stream. */
    private fun drainOutput(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo,
        out: ByteArrayOutputStream,
    ): Boolean {
        val buffer = requireNotNull(codec.getOutputBuffer(index))
        val bytes = ByteArray(info.size)
        buffer.position(info.offset)
        buffer.get(bytes, 0, info.size)
        out.write(bytes)
        codec.releaseOutputBuffer(index, false)
        return info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
    }

    private fun ByteArray.toShorts(): ShortArray {
        val shorts = ShortArray(size / BYTES_PER_SAMPLE)
        java.nio.ByteBuffer
            .wrap(this)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(shorts)
        return shorts
    }

    private companion object {
        const val TAG = "SpriteGameSounds"
        const val TIMEOUT_US = 10_000L
        const val BYTES_PER_SAMPLE = 2
        const val MILLIS_PER_SECOND = 1_000L
    }
}
