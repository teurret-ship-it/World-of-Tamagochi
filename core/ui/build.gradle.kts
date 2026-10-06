import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.wot.android.library)
    alias(libs.plugins.wot.android.compose)
}

android {
    namespace = "com.mymagicalpet.ui"
}

dependencies {
    api(projects.core.sim)
    implementation(projects.core.designsystem)
    implementation(libs.kotlinx.coroutines.core)
}

/**
 * SND UI sounds (snd.dev). Their terms forbid redistributing the unmodified
 * files as standalone assets, so they never enter the repository: the build
 * downloads the pinned npm package, checks its SHA-256 and embeds the sound
 * sprite in the APK (ADR-006).
 */
abstract class SndSoundsTask : DefaultTask() {
    @get:Input abstract val url: Property<String>

    @get:Input abstract val sha256: Property<String>

    @get:Input abstract val kit: Property<String>

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @get:Internal abstract val cacheFile: RegularFileProperty

    @get:Inject abstract val archives: ArchiveOperations

    @get:Inject abstract val files: FileSystemOperations

    @TaskAction
    fun fetch() {
        val tgz = cacheFile.get().asFile
        if (!tgz.exists() || tgz.sha256() != sha256.get()) {
            tgz.parentFile.mkdirs()
            URI(url.get()).toURL().openStream().use { input -> tgz.outputStream().use { input.copyTo(it) } }
        }
        val actual = tgz.sha256()
        check(actual == sha256.get()) { "snd-lib checksum mismatch: $actual" }
        files.sync {
            from(archives.tarTree(archives.gzip(tgz))) {
                include("package/assets/sounds/sprite/${kit.get()}/audioSprite.ogg")
                eachFile { path = "raw/snd_sprite.ogg" }
                includeEmptyDirs = false
            }
            into(outputDir)
        }
    }

    private fun java.io.File.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(readBytes()).joinToString("") { "%02x".format(it) }
}

val sndSounds =
    tasks.register<SndSoundsTask>("sndSounds") {
        url.set("https://registry.npmjs.org/snd-lib/-/snd-lib-1.2.4.tgz")
        sha256.set("81f9efd69aa9c85af28440e5725ccc1bc1721dfeed0c32797a84bb109742990e")
        kit.set("03")
        cacheFile.set(layout.buildDirectory.file("snd/snd-lib-1.2.4.tgz"))
        outputDir.set(layout.buildDirectory.dir("generated/snd/res"))
    }

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(sndSounds, SndSoundsTask::outputDir)
    }
}
