package com.worldoftamagochi.feature.shop

/** Display names of the shop items; a test checks every catalog item has one. */
internal val ITEM_NAMES: Map<String, Int> =
    mapOf(
        "ribbon" to R.string.item_ribbon,
        "blossom" to R.string.item_blossom,
        "bell" to R.string.item_bell,
        "glasses" to R.string.item_glasses,
        "cap" to R.string.item_cap,
        "scarf" to R.string.item_scarf,
        "butterfly" to R.string.item_butterfly,
        "sunglasses" to R.string.item_sunglasses,
        "grad_cap" to R.string.item_grad_cap,
        "goggles" to R.string.item_goggles,
        "headphones" to R.string.item_headphones,
        "gem" to R.string.item_gem,
        "top_hat" to R.string.item_top_hat,
        "crown" to R.string.item_crown,
    )

internal fun itemName(id: String): Int = ITEM_NAMES.getValue(id)
