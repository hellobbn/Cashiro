package com.ritesh.cashiro.data.update

data class GitHubRelease(
    val tag: String,
    val title: String,
    val htmlUrl: String,
    val publishedAtMillis: Long,
    val apkUrl: String?,
    val commitCount: Int = 0
)
