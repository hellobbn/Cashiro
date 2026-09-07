package com.ritesh.cashiro.presentation.ui.features.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.brokerage.BrokerageRepository
import com.ritesh.cashiro.data.brokerage.IbkrFlexProvider
import com.ritesh.cashiro.domain.brokerage.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InvestmentsViewModel @Inject constructor(private val repository: BrokerageRepository) : ViewModel() {
    private var operation: Job? = null
    val connections = repository.connections
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<BrokerageError?>(null)
    val error = _error.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    init { reload() }
    fun reload() = run { repository.load(); _loaded.value = true }
    fun connect(label: String, token: String, query: String, onSuccess: () -> Unit) = run {
        repository.connect(IbkrFlexProvider.ID, label, BrokerCredentials(mapOf("token" to token.trim(), "queryId" to query.trim())))
        onSuccess()
    }
    fun refresh(id: String) = run { repository.refresh(id) }
    fun disconnect(id: String) = run { repository.disconnect(id) }
    fun cancelConnection() { operation?.cancel() }
    fun clearError() { _error.value = null }

    private fun run(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        operation = viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: BrokerageException) { _error.value = e.error }
            catch (_: Exception) { _error.value = BrokerageError.STORAGE }
            finally { _busy.value = false }
        }
    }
}
