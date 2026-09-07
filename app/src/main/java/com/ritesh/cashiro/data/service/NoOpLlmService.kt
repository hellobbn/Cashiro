package com.ritesh.cashiro.data.service

import com.ritesh.cashiro.domain.service.LlmService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Singleton
class NoOpLlmService @Inject constructor() : LlmService {
    override suspend fun initialize(modelPath: String) = Result.success(Unit)
    override suspend fun generateResponse(prompt: String) =
        Result.failure(UnsupportedOperationException("On-device chat was removed"))
    override fun generateResponseStream(prompt: String): Flow<String> = emptyFlow()
    override suspend fun reset() {}
    override suspend fun resetConversation() {}
    override fun isInitialized() = false
}
