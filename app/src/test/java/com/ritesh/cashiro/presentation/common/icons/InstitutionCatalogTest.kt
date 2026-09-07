package com.ritesh.cashiro.presentation.common.icons

import org.junit.Assert.*
import org.junit.Test

class InstitutionCatalogTest {
    @Test fun `requested institutions resolve by familiar names`() {
        mapOf(
            "IBKR" to "ibkr", "Chase" to "chase", "BofA" to "bofa",
            "SoFi" to "sofi", "Schwab" to "schwab", "Capital One" to "capital_one",
            "AMEX US" to "amex_us", "BOC" to "boc", "OCBC (SG)" to "ocbc_sg",
            "HSBC HK" to "hsbc_hk", "BOC HK" to "bochk",
            "中国建设银行" to "ccb", "招商银行" to "cmb"
        ).forEach { (name, id) -> assertEquals(name, id, InstitutionCatalog.find(name)?.id) }
    }

    @Test fun `hong kong and mainland identities stay distinct`() {
        assertEquals("HKD", InstitutionCatalog.find("BOC HK savings")?.currency)
        assertEquals("CNY", InstitutionCatalog.find("BOC savings")?.currency)
        assertEquals("bochk", InstitutionCatalog.find("中國銀行香港")?.id)
        assertEquals("citic_cn", InstitutionCatalog.find("China CITIC Bank")?.id)
        assertEquals("citi_us", InstitutionCatalog.find("Citibank US")?.id)
    }

    @Test fun `search supports Chinese abbreviations and multiple words`() {
        assertEquals("ccb", InstitutionCatalog.search("建行").single().id)
        assertEquals("bofa", InstitutionCatalog.search("bank america").single().id)
        assertEquals("ibkr", InstitutionCatalog.search("IBKR US").single().id)
        assertTrue(InstitutionCatalog.search("boc", "US").isEmpty())
        assertTrue(InstitutionCatalog.search("", "SG").all { it.currency == "SGD" })
    }

    @Test fun `short aliases do not match unrelated merchants`() {
        listOf("boat shop", "abcde", "chased delivery", "citizen", "", "   ")
            .forEach { assertNull(it, InstitutionCatalog.find(it)) }
    }

    @Test fun `persisted identity names and IDs are unique`() {
        val institutions = InstitutionCatalog.institutions
        assertEquals(institutions.size, institutions.map { it.id }.distinct().size)
        assertEquals(institutions.size, institutions.map { it.iconName }.distinct().size)
    }
}
