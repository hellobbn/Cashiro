package com.ritesh.cashiro.data.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.gitHubUpdateStore by preferencesDataStore(name = "github_update")

@Singleton
class GitHubUpdatePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dismissedCount = intPreferencesKey("dismissed_github_commit_count")

    private val dismissedRelease = intPreferencesKey("dismissed_github_release_version_code")
    private fun key(channel: PublishChannel) = if (channel == PublishChannel.DEBUG) dismissedCount else dismissedRelease

    suspend fun getDismissedBuild(channel: PublishChannel): Int {
        return context.gitHubUpdateStore.data.map { it[key(channel)] ?: 0 }.first()
    }

    suspend fun setDismissedBuild(channel: PublishChannel, build: Int) {
        context.gitHubUpdateStore.edit { it[key(channel)] = build }
    }
}
