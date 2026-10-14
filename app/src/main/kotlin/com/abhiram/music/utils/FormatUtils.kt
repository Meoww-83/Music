package com.abhiram.music.utils

/**
 * Thumbnail helpers for YouTube Music images.
 *
 * InnerTube returns `i.ytimg.com/vi/<id>/...` style URLs at several fixed resolutions; the
 * standard music library only ships a single (usually 60 px wide) URL per item, so we rewrite it
 * to the highest useful size instead of downloading dozens of variants.
 */

private const val DEFAULT_WIDTH = 1080

/** Upgrades a YouTube thumbnail URL to [width]x[height] (defaults to 1080p). */
fun String.resize(
    width: Int = DEFAULT_WIDTH,
    height: Int = DEFAULT_WIDTH,
): String {
    if (isEmpty()) return this
    // Only rewrite known YouTube image hosts, never arbitrary URLs.
    val host = substringAfter("://", "").substringBefore('/')
    if (host != "i.ytimg.com" && host != "img.youtube.com" && host != "yt3.ggpht.com") {
        if (host.endsWith("ggpht.com")) {
            // yt3 channel avatars use =w<width>-h<height> style parameters.
            return substringBefore("=") + "=w$width-h$height"
        }
        return this
    }
    return when {
        contains("=w") || contains("=s") -> substringBefore("=") + "=w$width-h$height"
        else -> "$this=w$width-h$height"
    }
}

/** Formats a millisecond position as `m:ss` / `h:mm:ss`. */
fun Long.formatDuration(): String {
    val totalSeconds = (this / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:%02d:%02d".format(minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Formats an Int number of seconds as `m:ss`. */
fun Int.formatDuration(): String = toLong().formatDuration()

/** Formats a byte count as a human readable size (e.g. `1.4 GB`). */
fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = this.toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}

/** Collapses a list of artist names into a single comma separated line. */
fun List<String>.joinArtists(): String = joinToString(", ")
