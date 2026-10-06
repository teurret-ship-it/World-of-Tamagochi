package com.mymagicalpet.sim

/**
 * The names a pet can have. Picking from a list instead of typing keeps
 * children's games free of personal data and bad words by design (CLAUDE.md
 * section 2): nothing to filter, nothing to report. Online the name shows as
 * "Mochi #4821", so two Mochis never look the same.
 */
object PetNames {
    val ALL: List<String> =
        listOf(
            "Mochi",
            "Pip",
            "Bubbles",
            "Peanut",
            "Sprout",
            "Biscuit",
            "Pebble",
            "Noodle",
            "Maple",
            "Pudding",
            "Waffles",
            "Comet",
            "Pickle",
            "Clover",
            "Muffin",
            "Ziggy",
            "Puddle",
            "Tofu",
            "Nugget",
            "Sunny",
            "Bean",
            "Fizz",
            "Taco",
            "Juniper",
            "Marble",
            "Poppy",
            "Domino",
            "Kiwi",
            "Dumpling",
            "Pretzel",
            "Button",
            "Cosmo",
            "Twix",
            "Daisy",
            "Gizmo",
            "Honey",
            "Jelly",
            "Lolli",
            "Mango",
            "Nimbus",
            "Olive",
            "Pixel",
            "Quill",
            "Rascal",
            "Sushi",
            "Tango",
            "Umber",
            "Velvet",
            "Wiggles",
            "Yoyo",
        )

    const val DEFAULT = "Mochi"

    fun isAllowed(name: String): Boolean = name in ALL
}
