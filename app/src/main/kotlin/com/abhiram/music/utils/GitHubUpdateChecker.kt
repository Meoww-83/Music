package com.abhiram.music.utils

import com.abhiram.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of a successful GitHub release lookup.
 *
 * @property versionName release tag, normalised without the leading `v` (e.g. `1.4.0`).
 * @property htmlUrl release page the "Update" button opens.
 * @property body release notes (may be empty).
 */
data class UpdateInfo(
    val versionName: String,
    val htmlUrl: String,
    val body: String?,
)

/**
 * Checks the GitHub releases of [BuildConfig.GITHUB_REPOSITORY] and reports the newest tag when it
 * is newer than the installed version.
 *
 * The check is a plain HTTPS GET of the public `releases/latest` endpoint — no authentication, no
 * extra dependencies (HttpURLConnection + org.json which Android ships in the platform).
 */
object GitHubUpdateChecker {
    private const val REQUEST_TIMEOUT_MS = 15_000

    /**
     * @return the newest release when it is strictly newer than [currentVersion], `null` when the
     * app is up to date (or the lookup failed — an unreachable network must never block app start).
     */
    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
    ): UpdateInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPOSITORY}/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = REQUEST_TIMEOUT_MS
                connection.readTimeout = REQUEST_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Music-Updater/${BuildConfig.VERSION_NAME}")
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name").removePrefix("v").trim()
                    if (tag.isEmpty()) return@runCatching null
                    if (!isNewer(tag, currentVersion)) return@runCatching null
                    UpdateInfo(
                        versionName = tag,
                        htmlUrl = json.optString("html_url").ifEmpty {
                            "https://github.com/${BuildConfig.GITHUB_REPOSITORY}/releases/latest"
                        },
                        body = json.optString("body").takeIf { it.isNotBlank() },
                    )
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    /** Semantic-ish comparison: `1.10.0` > `1.9.0` > `1.9.0-beta`. */
    fun isNewer(
        candidate: String,
        current: String,
    ): Boolean {
        val a = candidate.toVersionParts()
        val b = current.toVersionParts()
        for (i in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        // Equal numbers: a release tag without a suffix beats a pre-release of the same version.
        return candidate.toQualifier() == null && current.toQualifier() != null
    }

    private fun String.toVersionParts(): List<Int> =
        substringBefore('-')
            .substringBefore('+')
            .split('.')
            .map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }

    private fun String.toQualifier(): String? = substringAfter('-', "").ifEmpty { null }
}
