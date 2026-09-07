package com.ritesh.cashiro.data.update

/** Build-time identity: never fall back to another package/signing channel. */
enum class PublishChannel(val id: String, val releasePath: String, val pagePath: String) {
    DEBUG("debug", "tags/debug-latest", "tag/debug-latest"),
    RELEASE("release", "latest", "latest");

    val apiUrl get() = "https://api.github.com/repos/hellobbn/Cashiro/releases/$releasePath"
    val pageUrl get() = "https://github.com/hellobbn/Cashiro/releases/$pagePath"

    fun acceptsRelease(tag: String, draft: Boolean, prerelease: Boolean): Boolean =
        !draft && when (this) {
            DEBUG -> tag == "debug-latest"
            RELEASE -> !prerelease && tag.startsWith("v")
        }

    fun acceptsAsset(name: String): Boolean = name.endsWith(".apk", ignoreCase = true) &&
        when (this) {
            DEBUG -> name.startsWith("Cashiro-debug-", ignoreCase = true)
            RELEASE -> name.startsWith("Cashiro-v", ignoreCase = true)
        }

    fun remoteBuild(release: GitHubRelease): Int = when (this) {
        DEBUG -> release.commitCount
        RELEASE -> release.versionCode
    }

    companion object {
        fun fromId(id: String): PublishChannel = entries.single { it.id == id }
        fun parseVersionCode(body: String?): Int =
            Regex("(?m)^version_code:\\s*(\\d+)\\s*$").find(body.orEmpty())
                ?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 } ?: 0
    }
}
