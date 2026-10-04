package com.ritesh.cashiro.presentation.ui.features.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.ai.AiAttachment
import com.ritesh.cashiro.data.ai.AiAttachmentReader
import com.ritesh.cashiro.data.ai.AiConfig
import com.ritesh.cashiro.data.ai.AiException
import com.ritesh.cashiro.data.ai.AiChat
import com.ritesh.cashiro.data.ai.AiLedgerSession
import com.ritesh.cashiro.data.ai.AiModel
import com.ritesh.cashiro.data.ai.AiSettings
import com.ritesh.cashiro.data.ai.AppliedChanges
import com.ritesh.cashiro.data.ai.LedgerChange
import com.ritesh.cashiro.data.ai.LedgerTools
import com.ritesh.cashiro.data.ai.PdfPasswordRequired
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Files shared to Cashiro, waiting for the AI screen to pick them up. */
@Singleton
class AiShareInbox @Inject constructor() {
    private val _pending = MutableStateFlow<List<Uri>>(emptyList())
    val pending: StateFlow<List<Uri>> = _pending.asStateFlow()

    fun offer(uris: List<Uri>) {
        if (uris.isNotEmpty()) _pending.update { it + uris }
    }

    fun take(): List<Uri> = _pending.value.also { _pending.value = emptyList() }
}

/** The provider's models, loaded once a key is entered. */
data class ModelListState(
    val loading: Boolean = false,
    val models: List<AiModel> = emptyList(),
    val error: String? = null
)

data class ReviewItem(val change: LedgerChange, val selected: Boolean)

sealed interface AiPhase {
    data object Compose : AiPhase
    data class Running(val proposed: Int) : AiPhase
    data object Review : AiPhase
    data class Saved(val applied: AppliedChanges) : AiPhase
}

data class AiAssistantUiState(
    val config: AiConfig = AiConfig(),
    val attachments: List<AiAttachment> = emptyList(),
    val request: String = "",
    val phase: AiPhase = AiPhase.Compose,
    val review: List<ReviewItem> = emptyList(),
    val summary: String = "",
    val error: String? = null,
    // A PDF that needs its password before it can be read
    val passwordFor: String? = null,
    val models: ModelListState = ModelListState()
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val settings: AiSettings,
    private val reader: AiAttachmentReader,
    private val session: AiLedgerSession,
    private val tools: LedgerTools,
    private val inbox: AiShareInbox,
    private val chat: AiChat
) : ViewModel() {
    private val _state = MutableStateFlow(AiAssistantUiState(config = settings.config.value))
    val state: StateFlow<AiAssistantUiState> = _state.asStateFlow()

    private val passwords = mutableMapOf<String, String>()
    private var lastDefaultRequest = ""
    private var running: Job? = null
    private var modelsJob: Job? = null

    init {
        viewModelScope.launch {
            inbox.pending.collect { if (it.isNotEmpty()) addFiles(inbox.take()) }
        }
    }

    fun saveConfig(config: AiConfig) {
        settings.save(config)
        _state.update { it.copy(config = settings.config.value) }
    }

    /** Lists the models [config]'s key can use; a newer call replaces one still loading. */
    fun loadModels(config: AiConfig) {
        modelsJob?.cancel()
        modelsJob = viewModelScope.launch {
            _state.update { it.copy(models = it.models.copy(loading = true, error = null)) }
            val result = try {
                ModelListState(models = chat.listModels(config))
            } catch (e: AiException) {
                ModelListState(error = e.message)
            }
            _state.update { it.copy(models = result) }
        }
    }

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                val copied = reader.copy(uris)
                _state.update { s ->
                    s.copy(attachments = s.attachments + copied, phase = AiPhase.Compose, error = null)
                }
            } catch (e: AiException) {
                _state.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun removeAttachment(attachment: AiAttachment) {
        _state.update { it.copy(attachments = it.attachments - attachment) }
    }

    fun setRequest(text: String) {
        _state.update { it.copy(request = text) }
    }

    /** Sends the files and request to the model. [defaultRequest] is used when the box is empty. */
    fun analyze(defaultRequest: String) {
        lastDefaultRequest = defaultRequest
        val s = _state.value
        if (running?.isActive == true || (s.attachments.isEmpty() && s.request.isBlank())) return
        running = viewModelScope.launch {
            _state.update { it.copy(phase = AiPhase.Running(0), error = null, passwordFor = null) }
            try {
                val parts = reader.parts(s.attachments, passwords)
                val proposal = session.run(parts, s.request.ifBlank { defaultRequest }) { proposed ->
                    _state.update { it.copy(phase = AiPhase.Running(proposed)) }
                }
                _state.update {
                    it.copy(
                        phase = AiPhase.Review,
                        summary = proposal.summary,
                        // Likely duplicates start unticked
                        review = proposal.changes.map { change ->
                            ReviewItem(change, selected = (change as? LedgerChange.Add)?.draft?.possibleDuplicate == null)
                        }
                    )
                }
            } catch (e: PdfPasswordRequired) {
                _state.update { it.copy(phase = AiPhase.Compose, passwordFor = e.name) }
            } catch (e: AiException) {
                _state.update { it.copy(phase = AiPhase.Compose, error = e.message) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update { it.copy(phase = AiPhase.Compose) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(phase = AiPhase.Compose, error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun cancel() {
        running?.cancel()
    }

    fun submitPassword(password: String?) {
        val name = _state.value.passwordFor ?: return
        if (password == null) {
            _state.update { it.copy(passwordFor = null) }
            return
        }
        passwords[name] = password
        analyze(lastDefaultRequest)
    }

    fun toggle(index: Int) {
        _state.update { s ->
            s.copy(review = s.review.mapIndexed { i, item -> if (i == index) item.copy(selected = !item.selected) else item })
        }
    }

    fun save() {
        val chosen = _state.value.review.filter { it.selected }.map { it.change }
        if (chosen.isEmpty()) return
        viewModelScope.launch {
            try {
                val applied = tools.apply(chosen)
                _state.update { it.copy(phase = AiPhase.Saved(applied)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    fun undo() {
        val applied = (_state.value.phase as? AiPhase.Saved)?.applied ?: return
        viewModelScope.launch {
            tools.undo(applied)
            _state.update { it.copy(phase = AiPhase.Review) }
        }
    }

    /** Back to an empty request, keeping the provider settings. */
    fun startOver() {
        passwords.clear()
        _state.update { AiAssistantUiState(config = it.config) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
