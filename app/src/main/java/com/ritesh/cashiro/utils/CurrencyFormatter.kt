package com.ritesh.cashiro.utils

import com.ritesh.cashiro.data.currency.model.CurrencySymbols
import com.ritesh.parser.core.bank.BankParserFactory
import com.ritesh.cashiro.presentation.common.icons.InstitutionCatalog
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Utility class for formatting currency values
 */
object CurrencyFormatter {

    private val INDIAN_LOCALE = Locale("en", "IN")

    /**
     * Locale mapping for different currencies
     */
    private val CURRENCY_LOCALES = mapOf(
        "INR" to INDIAN_LOCALE,
        "USD" to Locale.US,
        "EUR" to Locale.GERMANY,
        "GBP" to Locale.UK,
        "AED" to Locale.Builder().setLanguage("en").setRegion("AE").build(),
        "SGD" to Locale.Builder().setLanguage("en").setRegion("SG").build(),
        "CAD" to Locale.CANADA,
        "AUD" to Locale.Builder().setLanguage("en").setRegion("AU").build(),
        "JPY" to Locale.JAPAN,
        "CNY" to Locale.CHINA,
        "HKD" to Locale("en", "HK"),
        "TWD" to Locale.TAIWAN,
        "MOP" to Locale("zh", "MO"),
        "NPR" to Locale.Builder().setLanguage("ne").setRegion("NP").build(),
        "ETB" to Locale.Builder().setLanguage("am").setRegion("ET").build(),
        "THB" to Locale.Builder().setLanguage("th").setRegion("TH").build(),
        "MYR" to Locale.Builder().setLanguage("ms").setRegion("MY").build(),
        "KWD" to Locale.Builder().setLanguage("en").setRegion("KW").build(),
        "KRW" to Locale.KOREA,
        "SEK" to Locale.Builder().setLanguage("sv").setRegion("SE").build(),
        "CHF" to Locale.Builder().setLanguage("de").setRegion("CH").build(),
        "NZD" to Locale.Builder().setLanguage("en").setRegion("NZ").build(),
        "MXN" to Locale.Builder().setLanguage("es").setRegion("MX").build(),
        "TRY" to Locale("tr", "TR"),
        "RUB" to Locale("ru", "RU"),
        "ZAR" to Locale("en", "ZA"),
        "BRL" to Locale("pt", "BR"),
        "PLN" to Locale("pl", "PL"),
        "NOK" to Locale("nb", "NO"),
        "DKK" to Locale("da", "DK"),
        "CZK" to Locale("cs", "CZ"),
        "HUF" to Locale("hu", "HU"),
        "ILS" to Locale("he", "IL"),
        "PHP" to Locale("en", "PH"),
        "IDR" to Locale("in", "ID"),
        "SAR" to Locale("ar", "SA"),
        "COP" to Locale("es", "CO"),
        "KES" to Locale("sw", "KE")
    )

    private val DEFAULT_LOCALE = Locale.US

    /**
     * Formats a BigDecimal amount as currency with the specified currency code
     */
    fun formatCurrency(amount: BigDecimal, currencyCode: String = "CNY"): String {
        return try {
            val locale = CURRENCY_LOCALES[currencyCode] ?: if (currencyCode == "INR" || currencyCode == "NPR") INDIAN_LOCALE else DEFAULT_LOCALE
            val formatter = NumberFormat.getCurrencyInstance(locale)
            
            // Get our custom symbol
            val customSymbol = CurrencySymbols.getSymbol(currencyCode)

            // Configure formatting rules
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 2
 
            // Set the currency if supported
            try {
                formatter.currency = Currency.getInstance(currencyCode)
            } catch (e: Exception) {
                // If currency not supported, use symbol mapping
                return "$customSymbol${formatAmount(amount)}"
            }
 
            val formatted = formatter.format(amount)
            
            // If the formatted string doesn't contain our custom symbol, or contains the ISO code,
            // we override it to ensure the custom symbol is used.
            if (formatted.contains(currencyCode) || !formatted.contains(customSymbol)) {
                val cleanAmount = formatAmount(amount, currencyCode)
                return if (locale == Locale.US || locale == INDIAN_LOCALE || locale == Locale.UK || currencyCode in setOf("CNY", "HKD", "SGD", "TWD", "MOP", "JPY")) {
                    "$customSymbol$cleanAmount"
                } else {
                    "$cleanAmount $customSymbol"
                }
            }
            
            formatted
        } catch (e: Exception) {
            // Fallback to symbol + amount
            val symbol = CurrencySymbols.getSymbol(currencyCode)
            "$symbol${formatAmount(amount, currencyCode)}"
        }
    }

    /**
     * Formats a Double amount as currency with the specified currency code
     */
    fun formatCurrency(amount: Double, currencyCode: String = "CNY"): String {
        return formatCurrency(amount.toBigDecimal(), currencyCode)
    }

    /**
     * Formats an amount with proper grouping and decimals
     * Uses Indian grouping (#,##,##0.00) for INR and NPR, standard (#,##0.00) otherwise
     */
    fun formatAmount(amount: BigDecimal, currencyCode: String = "CNY"): String {
        val locale = CURRENCY_LOCALES[currencyCode] ?: if (currencyCode == "INR" || currencyCode == "NPR") INDIAN_LOCALE else DEFAULT_LOCALE
        val pattern = if (currencyCode == "INR" || currencyCode == "NPR") "#,##,##0.00" else "#,##0.00"
        val symbols = DecimalFormatSymbols(locale)
        val formatter = DecimalFormat(pattern, symbols)
        return formatter.format(amount)
    }

    /**
     * Formats a double amount with proper grouping and decimals
     */
    fun formatAmount(amount: Double, currencyCode: String = "CNY"): String {
        return formatAmount(amount.toBigDecimal(), currencyCode)
    }

    /**
     * Get symbol for a currency code
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return CurrencySymbols.getSymbol(currencyCode)
    }

    /**
     * Gets the base currency for a bank using the BankParserFactory
     * Returns CNY as default for unknown banks
     */
    fun getBankBaseCurrency(bankName: String?): String {
        if (bankName == null) return "CNY"
        // Unqualified legacy identities keep their historical parser currency. Regional
        // picker titles (for example AMEX US and HSBC HK) are deliberately explicit.
        val legacyIdentity = bankName.trim().lowercase(Locale.ROOT) in setOf(
            "amex", "american express", "citi", "citibank", "citi bank", "dbs", "dbs bank", "hsbc", "hsbc bank"
        )
        if (legacyIdentity) {
            return runCatching { BankParserFactory.getParser(bankName)?.getCurrency() }.getOrNull() ?: "INR"
        }
        InstitutionCatalog.find(bankName)?.let { return it.currency }

        // Try to find a parser that can handle this bank name
        return try {
            val parser = BankParserFactory.getParser(bankName)
            parser?.getCurrency() ?: "CNY"
        } catch (e: Exception) {
            "CNY"
        }
    }
}