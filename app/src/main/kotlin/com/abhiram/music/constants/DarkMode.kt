package com.abhiram.music.constants

/**
 * Dark-mode preference consumed by [com.abhiram.music.ui.theme.ExpressiveTheme].
 *
 * * [SYSTEM] — follow the system UI mode.
 * * [LIGHT] — always render the light scheme.
 * * [DARK] — always render the dark scheme.
 */
enum class DarkMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        /** Parses a persisted name, falling back to [SYSTEM] for unknown/legacy values. */
        fun fromName(name: String?): DarkMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
