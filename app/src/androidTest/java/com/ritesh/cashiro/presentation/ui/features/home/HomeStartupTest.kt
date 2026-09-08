package com.ritesh.cashiro.presentation.ui.features.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.MainActivity
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.testing.HomeTestDependencies
import dagger.hilt.android.EntryPointAccessors
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Uses the actual MainActivity -> HomeScreen -> Hilt HomeViewModel -> Room/DataStore chain. */
class HomeStartupTest {
    @get:Rule val rule = createEmptyComposeRule()
    private lateinit var deps: HomeTestDependencies
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun seedOnlyTheIsolatedSandbox() = runBlocking {
        // Deliberately refuse to seed the user's real Debug/Release database or a generic CI app.
        assumeTrue("Requires the local isolated Gradle init script", context.packageName == "com.ritesh.cashiro.lagcheck.debug")
        deps = EntryPointAccessors.fromApplication(context.applicationContext, HomeTestDependencies::class.java)
        withContext(Dispatchers.IO) { deps.database().clearAllTables() }
        deps.preferences().markScanTutorialShown()
        deps.preferences().updateUserName("Home startup fixture")
        deps.preferences().updateBaseCurrency("CNY")
        deps.preferences().updateBlurEffects(true)
        val now = LocalDateTime.now()
        val expiry = now.plusYears(1)
        deps.database().exchangeRateDao().insertExchangeRate(ExchangeRateEntity(
            fromCurrency = "USD", toCurrency = "CNY", rate = BigDecimal("7"), provider = "custom",
            updatedAt = now, updatedAtUnix = now.atZone(ZoneId.systemDefault()).toEpochSecond(),
            expiresAt = expiry, expiresAtUnix = expiry.atZone(ZoneId.systemDefault()).toEpochSecond(), isCustom = true
        ))
        deps.database().transactionDao().insertTransactions((0 until 1000).map { i ->
            TransactionEntity(id = i + 1L, amount = BigDecimal("10"), merchantName = "HOME_RECENT_$i",
                category = "Shopping", transactionType = TransactionType.EXPENSE,
                dateTime = now.minusMinutes(i.toLong()), transactionHash = "home-startup-$i",
                currency = if (i == 0) "USD" else "CNY", isSample = true)
        })
        repeat(8) { i ->
            deps.database().accountBalanceDao().insertBalance(AccountBalanceEntity(
                bankName = "Fixture bank $i", accountLast4 = i.toString().padStart(4, '0'),
                balance = BigDecimal("100"), timestamp = now, currency = if (i == 0) "USD" else "CNY",
                isCreditCard = i == 7, creditLimit = if (i == 7) BigDecimal("1000") else null, isSample = true
            ))
        }
    }

    @Test fun recentUsesSqlLimitAndExcludesDeletedAndBalanceUpdates() = runBlocking {
        val first = deps.database().transactionDao().getRecentTransactions(3).first().first()
        deps.database().transactionDao().insertTransactions(listOf(
            first.copy(id = 2001, transactionHash = "deleted-newest", dateTime = first.dateTime.plusMinutes(1), isDeleted = true),
            first.copy(id = 2002, transactionHash = "balance-newest", dateTime = first.dateTime.plusMinutes(2), transactionType = TransactionType.BALANCE_UPDATE)
        ))
        assertEquals(listOf(1L, 2L, 3L), deps.database().transactionDao().getRecentTransactions(3).first().map { it.id })
        assertTrue(deps.database().transactionDao().getRecentTransactions(0).first().isEmpty())
        assertEquals(1000, deps.database().transactionDao().getRecentTransactions(2000).first().size)
    }

    @Test fun realHomeLoadsAndKeepsRecentAcrossProfileUpdatesAndColdStarts() {
        repeat(2) { launch ->
            ActivityScenario.launch(MainActivity::class.java).use {
                rule.waitUntil(15_000) {
                    rule.onAllNodesWithTag("home_content_list").fetchSemanticsNodes().isNotEmpty()
                }
                val list = rule.onNodeWithTag("home_content_list")
                list.performScrollToNode(hasText(context.getString(R.string.recent)))
                rule.waitUntil(15_000) {
                    rule.onAllNodesWithText("HOME_RECENT_0").fetchSemanticsNodes().isNotEmpty()
                }
                list.performScrollToNode(hasText("HOME_RECENT_0"))
                rule.onNodeWithText("HOME_RECENT_0").assertIsDisplayed()
                rule.onNodeWithTag("home_recent_loading").assertDoesNotExist()
                runBlocking { deps.preferences().updateUserName("Home startup fixture $launch") }
                rule.waitForIdle()
                rule.onNodeWithTag("home_recent_loading").assertDoesNotExist()
                list.performTouchInput { swipeUp(durationMillis = 350) }
                list.performTouchInput { swipeDown(durationMillis = 350) }
                list.performScrollToNode(hasText(context.getString(R.string.recent)))
                list.performScrollToNode(hasText("HOME_RECENT_0"))
                rule.onNodeWithText("HOME_RECENT_0").assertIsDisplayed()
                rule.onNodeWithTag("home_recent_loading").assertDoesNotExist()
            }
        }
    }
}
