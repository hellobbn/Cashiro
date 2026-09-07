package com.ritesh.cashiro.data.service

import com.ritesh.cashiro.domain.service.LlmService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoOpLlmServiceTest {
    private val service: LlmService = NoOpLlmService()

    @Test
    fun `initialization succeeds without enabling the removed model`() = runBlocking {
        assertTrue(service.initialize("unused-model-path").isSuccess)
        assertFalse(service.isInitialized())
    }

    @Test
    fun `generation returns a typed failure instead of throwing`() = runBlocking {
        val result: Result<String> = service.generateResponse("hello")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
        assertEquals("On-device chat was removed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `streaming completes without emitting responses`() = runBlocking {
        assertTrue(service.generateResponseStream("hello").toList().isEmpty())
    }

    @Test
    fun `reset operations keep the removed model disabled`() = runBlocking {
        service.reset()
        service.resetConversation()

        assertFalse(service.isInitialized())
    }
}
