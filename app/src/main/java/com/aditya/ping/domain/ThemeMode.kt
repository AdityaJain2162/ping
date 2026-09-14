package com.aditya.ping.domain

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
