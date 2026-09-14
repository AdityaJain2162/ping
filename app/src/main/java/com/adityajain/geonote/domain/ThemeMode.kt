package com.adityajain.geonote.domain

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED;

    companion object {
        fun fromOrdinalSafe(value: Int): ThemeMode =
            entries.getOrElse(value) { SYSTEM }
    }
}
