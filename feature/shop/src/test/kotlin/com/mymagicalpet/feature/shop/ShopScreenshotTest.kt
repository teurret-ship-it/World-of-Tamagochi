package com.mymagicalpet.feature.shop

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mymagicalpet.designsystem.MagicTheme
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.shop.Catalog
import com.mymagicalpet.sim.shop.Slot
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ShopScreenshotTest {
    private fun state(
        coins: Long,
        owned: Set<String>,
        worn: Map<Slot, String>,
        selected: String?,
        seed: Long = 7,
    ) = ShopUiState(
        name = "Mochi",
        genome = Genome.fromSeed(seed),
        coins = coins,
        items = Catalog.ITEMS.map { ShopItem(it, it.id in owned, worn[it.slot] == it.id, coins >= it.price) },
        worn = worn,
        selectedId = selected,
    )

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) = captureRoboImage("src/test/screenshots/$name.png") { MagicTheme { content() } }

    @Test
    fun firstVisit() {
        capture("shop_first_visit") {
            ShopScreen(state(coins = 120, owned = emptySet(), worn = emptyMap(), selected = null), ShopActions(), animate = false)
        }
    }

    @Test
    fun tryingOnTheCrown() {
        val worn = mapOf(Slot.NECK to "bell", Slot.FACE to "glasses")
        capture("shop_try_on_crown") {
            ShopScreen(
                state(coins = 260, owned = setOf("ribbon", "bell", "glasses"), worn = worn, selected = "crown"),
                ShopActions(),
                animate = false,
            )
        }
    }

    @Test
    fun readyToBuy() {
        capture("shop_ready_to_buy") {
            ShopScreen(
                state(coins = 300, owned = setOf("ribbon"), worn = mapOf(Slot.HEAD to "ribbon"), selected = "top_hat"),
                ShopActions(),
                animate = false,
            )
        }
    }
}
