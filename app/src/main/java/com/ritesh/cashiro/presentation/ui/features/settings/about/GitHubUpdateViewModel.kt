package com.ritesh.cashiro.presentation.ui.features.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.BuildConfig
import com.ritesh.cashiro.data.update.GitHubRelease
import com.ritesh.cashiro.data.update.GitHubUpdatePreferences
import com.ritesh.cashiro.data.update.GitHubUpdateRepository
import com.ritesh.cashiro.data.update.PublishChannel
import com.ritesh.cashiro.data.update.UpdateAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GitHubUpdateUiState(
    val isChecking: Boolean = false,
    val available: GitHubRelease? = null,
    val showDialog: Boolean = false,
    val status: GitHubUpdateStatus = GitHubUpdateStatus.Idle
)

enum class GitHubUpdateStatus {
    Idle,
    Checking,
    UpToDate,
    Available,
    Failed
}

@HiltViewModel
class GitHubUpdateViewModel @Inject constructor(
    private val repository: GitHubUpdateRepository,
    private val preferences: GitHubUpdatePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitHubUpdateUiState())
    val uiState: StateFlow<GitHubUpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate(promptIfAvailable: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, status = GitHubUpdateStatus.Checking) }
            val result = repository.fetchLatestRelease()
            result.fold(
                onSuccess = { release ->
                    val prompt = UpdateAvailability.shouldPrompt(
                        remoteCommitCount = repository.channel.remoteBuild(release),
                        localCommitCount = if (repository.channel == PublishChannel.DEBUG) BuildConfig.GIT_COMMIT_COUNT else BuildConfig.VERSION_CODE,
                        dismissedCommitCount = preferences.getDismissedBuild(repository.channel)
                    )
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            available = release,
                            showDialog = promptIfAvailable && prompt,
                            status = if (prompt) GitHubUpdateStatus.Available else GitHubUpdateStatus.UpToDate
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            available = null,
                            showDialog = false,
                            status = GitHubUpdateStatus.Failed
                        )
                    }
                }
            )
        }
    }

    fun dismissDialog() {
        val release = _uiState.value.available
        viewModelScope.launch {
            if (release != null) {
                preferences.setDismissedBuild(repository.channel, repository.channel.remoteBuild(release))
            }
            _uiState.update { it.copy(showDialog = false) }
        }
    }

    fun hideDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }
}
