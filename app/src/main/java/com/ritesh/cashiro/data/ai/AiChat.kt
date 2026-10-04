package com.ritesh.cashiro.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** What the user hands the model. */
sealed interface AiPart {
    data class Text(val text: String) : AiPart
    data class Image(val mediaType: String, val base64: String) : AiPart
    // Claude reads the PDF itself; other providers get [text], the PDF's own text layer, or the
    // file itself when it has none (a scan), which OpenAI and OpenRouter accept
    data class Pdf(val name: String, val base64: String, val text: String) : AiPart
}

data class AiTool(val name: String, val description: String, val schema: JsonObject)
data class AiToolCall(val id: String, val name: String, val input: JsonObject)
data class AiToolResult(val callId: String, val content: String, val isError: Boolean = false)
data class AiReply(val text: String, val calls: List<AiToolCall>, val truncated: Boolean)

class AiException(message: String) : Exception(message)

/**
 * A conversation with a cloud model over plain HTTP, in either protocol. Messages stay in the
 * provider's own shape and the model's replies are kept verbatim (thinking blocks included), so
 * the history only ever grows by appending.
 */
class AiConversation(val config: AiConfig, val system: String, val tools: List<AiTool>) {
    internal val messages = mutableListOf<JsonObject>()
}

@Singleton
class AiChat internal constructor(engine: HttpClientEngine) {
    @Inject constructor() : this(Android.create())

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            // One non-streamed reply can take minutes on a long statement
            requestTimeoutMillis = 10 * 60_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 10 * 60_000
        }
        // No logging: requests carry the API key and the user's documents
    }

    fun addUser(conversation: AiConversation, parts: List<AiPart>) {
        conversation.messages += when (conversation.config.protocol) {
            AiProtocol.ANTHROPIC -> anthropicUser(parts)
            AiProtocol.OPENAI_COMPATIBLE -> openAiUser(parts)
        }
    }

    fun addToolResults(conversation: AiConversation, results: List<AiToolResult>) {
        when (conversation.config.protocol) {
            // All results in one user message, so the model keeps making parallel calls
            AiProtocol.ANTHROPIC -> conversation.messages += buildJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    results.forEach { r ->
                        addJsonObject {
                            put("type", "tool_result")
                            put("tool_use_id", r.callId)
                            put("content", r.content)
                            if (r.isError) put("is_error", true)
                        }
                    }
                }
            }
            AiProtocol.OPENAI_COMPATIBLE -> results.forEach { r ->
                conversation.messages += buildJsonObject {
                    put("role", "tool")
                    put("tool_call_id", r.callId)
                    put("content", if (r.isError) "Error: ${r.content}" else r.content)
                }
            }
        }
    }

    /** Sends the conversation, appends the model's reply to it and returns that reply. */
    suspend fun send(conversation: AiConversation): AiReply = when (conversation.config.protocol) {
        AiProtocol.ANTHROPIC -> sendAnthropic(conversation)
        AiProtocol.OPENAI_COMPATIBLE -> sendOpenAi(conversation)
    }

    // --- Claude Messages API ---

    private suspend fun sendAnthropic(c: AiConversation): AiReply {
        val official = runCatching { URI(c.config.baseUrl).host }.getOrNull() == "api.anthropic.com"
        // On a declined request the API retries on the model Anthropic recommends; only the
        // first-party API and the newest models take it, so proxies and older models go without.
        val fallbacks = official && c.config.model in FALLBACK_MODELS
        val body = buildJsonObject {
            put("model", c.config.model)
            put("max_tokens", 32_000)
            put("system", c.system)
            if (fallbacks) put("fallbacks", "default")
            if (c.tools.isNotEmpty()) putJsonArray("tools") {
                c.tools.forEach { t ->
                    addJsonObject {
                        put("name", t.name)
                        put("description", t.description)
                        put("input_schema", t.schema)
                    }
                }
            }
            put("messages", JsonArray(c.messages))
        }
        val response = post("${c.config.baseUrl}/v1/messages", body) {
            header("x-api-key", c.config.apiKey)
            header("anthropic-version", "2023-06-01")
            if (fallbacks) header("anthropic-beta", "server-side-fallback-2026-07-01")
        }
        val stop = response["stop_reason"]?.jsonPrimitive?.contentOrNull
        if (stop == "refusal") throw AiException("The model declined this request.")
        val content = response["content"]?.jsonArray ?: JsonArray(emptyList())
        c.messages += buildJsonObject {
            put("role", "assistant")
            put("content", content)
        }
        val blocks = content.map { it.jsonObject }
        return AiReply(
            text = blocks.filter { it.type == "text" }.joinToString("\n") { it.string("text") },
            calls = blocks.filter { it.type == "tool_use" }.map {
                AiToolCall(it.string("id"), it.string("name"), it["input"]?.jsonObject ?: JsonObject(emptyMap()))
            },
            truncated = stop == "max_tokens"
        )
    }

    private fun anthropicUser(parts: List<AiPart>) = buildJsonObject {
        put("role", "user")
        putJsonArray("content") {
            // Documents and images before the text that refers to them
            parts.sortedBy { if (it is AiPart.Text) 1 else 0 }.forEach { part ->
                addJsonObject {
                    when (part) {
                        is AiPart.Text -> {
                            put("type", "text")
                            put("text", part.text)
                        }
                        is AiPart.Image -> {
                            put("type", "image")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", part.mediaType)
                                put("data", part.base64)
                            }
                        }
                        is AiPart.Pdf -> {
                            put("type", "document")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", "application/pdf")
                                put("data", part.base64)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- OpenAI Chat Completions (and compatible providers) ---

    private suspend fun sendOpenAi(c: AiConversation): AiReply {
        val body = buildJsonObject {
            put("model", c.config.model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", c.system)
                }
                c.messages.forEach { add(it) }
            }
            if (c.tools.isNotEmpty()) putJsonArray("tools") {
                c.tools.forEach { t ->
                    addJsonObject {
                        put("type", "function")
                        putJsonObject("function") {
                            put("name", t.name)
                            put("description", t.description)
                            put("parameters", t.schema)
                        }
                    }
                }
            }
        }
        val response = post("${c.config.baseUrl}/chat/completions", body) {
            header("Authorization", "Bearer ${c.config.apiKey}")
        }
        val choice = response["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw AiException("The provider returned no answer.")
        val message = choice["message"]?.jsonObject ?: throw AiException("The provider returned no answer.")
        c.messages += message
        val calls = message["tool_calls"]?.let { it as? JsonArray }.orEmpty().map { el ->
            val call = el.jsonObject
            val function = call["function"]!!.jsonObject
            val arguments = function.string("arguments")
            AiToolCall(
                id = call.string("id"),
                name = function.string("name"),
                input = runCatching { json.parseToJsonElement(arguments).jsonObject }
                    .getOrElse { JsonObject(mapOf(INVALID_ARGUMENTS to JsonPrimitive(arguments))) }
            )
        }
        return AiReply(
            text = (message["content"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            calls = calls,
            truncated = choice["finish_reason"]?.jsonPrimitive?.contentOrNull == "length"
        )
    }

    private fun openAiUser(parts: List<AiPart>): JsonObject {
        val texts = parts.map { if (it is AiPart.Pdf && it.text.isNotBlank()) AiPart.Text("File ${it.name}:\n${it.text}") else it }
        return buildJsonObject {
            put("role", "user")
            // Text-only models (DeepSeek and others) reject content arrays: send plain text when there are no images
            if (texts.all { it is AiPart.Text }) {
                put("content", texts.joinToString("\n\n") { (it as AiPart.Text).text })
            } else {
                put("content", buildJsonArray {
                    texts.forEach { part ->
                        addJsonObject {
                            when (part) {
                                is AiPart.Text -> {
                                    put("type", "text")
                                    put("text", part.text)
                                }
                                is AiPart.Image -> {
                                    put("type", "image_url")
                                    putJsonObject("image_url") { put("url", "data:${part.mediaType};base64,${part.base64}") }
                                }
                                is AiPart.Pdf -> {
                                    put("type", "file")
                                    putJsonObject("file") {
                                        put("filename", part.name)
                                        put("file_data", "data:application/pdf;base64,${part.base64}")
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    // --- shared ---

    private suspend fun post(
        url: String,
        body: JsonObject,
        headers: io.ktor.client.request.HttpRequestBuilder.() -> Unit
    ): JsonObject {
        val response = try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                headers()
                setBody(body.toString())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AiException("Could not reach ${runCatching { URI(url).host }.getOrNull() ?: url}: ${e.javaClass.simpleName}")
        }
        val text = response.bodyAsText()
        val parsed = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        if (!response.status.isSuccess()) {
            val message = parsed?.get("error")?.let { error ->
                (error as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
                    ?: (error as? JsonPrimitive)?.contentOrNull
            }
            throw AiException("HTTP ${response.status.value}" + (message?.let { ": $it" } ?: ""))
        }
        return parsed ?: throw AiException("The provider sent a response that is not JSON.")
    }

    private val JsonObject.type get() = string("type")
    private fun JsonObject.string(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

    companion object {
        /** Set on a call whose arguments were not valid JSON, holding the raw text. */
        const val INVALID_ARGUMENTS = "_invalid_arguments"
        private val FALLBACK_MODELS = setOf("claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5", "claude-fable-5-1")
    }
}

