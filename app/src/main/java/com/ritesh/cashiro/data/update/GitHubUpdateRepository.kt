package com.ritesh.cashiro.data.update

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Singleton
class GitHubUpdateRepository @Inject constructor() {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }
            )
        }
    }

    suspend fun fetchLatestRelease(): Result<GitHubRelease> {
        return runCatching {
            fetchRelease(DEBUG_TAG_URL) ?: fetchRelease(LATEST_URL)
                ?: error("No GitHub release found")
        }
    }

    private suspend fun fetchRelease(url: String): GitHubRelease? {
        val response = client.get(url) {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header("X-GitHub-Api-Version", "2022-11-28")
        }
        if (!response.status.isSuccess()) {
            Log.i(TAG, "GitHub release $url -> ${response.status}")
            return null
        }
        val payload = response.body<GitHubReleaseDto>()
        if (payload.draft) return null
        val publishedAt = payload.publishedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
        val title = payload.name?.takeIf { it.isNotBlank() } ?: payload.tagName.orEmpty()
        return GitHubRelease(
            tag = payload.tagName.orEmpty(),
            title = title,
            htmlUrl = payload.htmlUrl.orEmpty(),
            publishedAtMillis = publishedAt,
            apkUrl = pickApkUrl(payload.assets),
            commitCount = DebugBuildIdentity.parseCommitCount(
                title = title,
                body = payload.body,
                assetNames = payload.assets.map { it.name }
            )
        )
    }

    private fun pickApkUrl(assets: List<GitHubAssetDto>): String? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) && !it.name.contains("fdroid", ignoreCase = true) }
        return apks.firstOrNull { it.name.contains("arm64") }?.browserDownloadUrl
            ?: apks.firstOrNull { it.name.contains("universal") }?.browserDownloadUrl
            ?: apks.firstOrNull()?.browserDownloadUrl
    }

    @Serializable
    private data class GitHubReleaseDto(
        @SerialName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val body: String? = null,
        @SerialName("html_url") val htmlUrl: String? = null,
        @SerialName("published_at") val publishedAt: String? = null,
        val draft: Boolean = false,
        val assets: List<GitHubAssetDto> = emptyList()
    )

    @Serializable
    private data class GitHubAssetDto(
        val name: String = "",
        @SerialName("browser_download_url") val browserDownloadUrl: String? = null
    )

    companion object {
        private const val TAG = "GitHubUpdate"
        private const val USER_AGENT = "Cashiro-UpdateCheck"
        const val OWNER_REPO = "hellobbn/Cashiro"
        const val DEBUG_TAG = "debug-latest"
        private const val DEBUG_TAG_URL = "https://api.github.com/repos/$OWNER_REPO/releases/tags/$DEBUG_TAG"
        private const val LATEST_URL = "https://api.github.com/repos/$OWNER_REPO/releases/latest"
        const val RELEASES_PAGE = "https://github.com/$OWNER_REPO/releases/tag/$DEBUG_TAG"
    }
}
