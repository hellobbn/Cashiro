package com.ritesh.cashiro.domain.brokerage

import kotlinx.serialization.Serializable

/** Monetary and quantity values are decimal strings, never binary floating point. */
@Serializable
data class Holding(
    val instrumentId: String,
    val symbol: String,
    val description: String,
    val currency: String,
    val quantity: String,
    val marketValue: String? = null,
    val costBasis: String? = null,
    val unrealizedPnl: String? = null,
    val assetClass: String = "",
    val model: String = ""
)

@Serializable
data class BrokerageAccount(
    val accountId: String,
    val asOf: String,
    val holdings: List<Holding>
)

/** Providers translate their API into a common, read-only snapshot. No trading methods. */
interface BrokerageProvider {
    val id: String
    suspend fun fetchHoldings(credentials: BrokerCredentials): List<BrokerageAccount>
}

@Serializable
class BrokerCredentials(val fields: Map<String, String>) {
    override fun toString() = "BrokerCredentials([REDACTED])"
}

enum class BrokerageError {
    INVALID_CREDENTIALS, EXPIRED_CREDENTIALS, IP_RESTRICTED, INVALID_QUERY,
    REPORT_NOT_READY, RATE_LIMITED, NETWORK, INVALID_REPORT, UNSUPPORTED_PROVIDER,
    STORAGE, DUPLICATE_CONNECTION
}

/** Only a controlled error category crosses into UI; never a token, URL or raw server error. */
class BrokerageException(val error: BrokerageError) : Exception(error.name)
