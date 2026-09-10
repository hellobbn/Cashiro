package com.ritesh.cashiro.presentation.ui.features.settings.quicktemplates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.QuickTemplateEntity
import com.ritesh.cashiro.data.repository.QuickTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class QuickTemplatesViewModel @Inject constructor(
    private val repository: QuickTemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<QuickTemplateEntity>> = repository.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun update(template: QuickTemplateEntity, name: String, amount: BigDecimal?, prefillAmount: Boolean) {
        viewModelScope.launch {
            repository.update(
                template.copy(
                    name = name.trim().ifBlank { template.merchantName },
                    amount = amount,
                    prefillAmount = prefillAmount && amount != null
                )
            )
        }
    }

    fun move(template: QuickTemplateEntity, delta: Int) {
        val list = templates.value.toMutableList()
        val from = list.indexOfFirst { it.id == template.id }
        val to = from + delta
        if (from < 0 || to < 0 || to >= list.size) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { repository.reorder(list) }
    }
}
