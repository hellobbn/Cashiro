package com.ritesh.cashiro.utils

import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.common.icons.BrandIcons
import org.junit.Assert.*
import org.junit.Test

class BrandLookupPerformanceRegressionTest {
    @Test fun longerBrandWinsOverGenericBrand() {
        assertEquals(R.drawable.ic_brand_google_pay, BrandIcons.getIconResource("GOOGLE PAY purchase"))
        assertEquals(R.drawable.ic_brand_youtube_music, BrandIcons.getIconResource("YouTube Music subscription"))
        assertEquals(R.drawable.ic_brand_google, BrandIcons.getIconResource("Google"))
    }

    @Test fun credExceptionDoesNotMatchCredit() {
        assertEquals(R.drawable.ic_brand_cred, BrandIcons.getIconResource("CRED payment"))
        assertNotEquals(R.drawable.ic_brand_cred, BrandIcons.getIconResource("Credit payment"))
    }

    @Test fun emptyAndUnknownNamesRetainFallback() {
        assertNull(BrandIcons.getIconResource(""))
        assertNull(BrandIcons.getIconResource("qwerty 987654"))
    }

    @Test fun repeatedLookupHasStablePrecedence() {
        repeat(1000) {
            assertEquals(R.drawable.ic_brand_google_pay, BrandIcons.getIconResource("Google Pay"))
            assertEquals(R.drawable.ic_brand_youtube_music, BrandIcons.getIconResource("YouTube Music"))
        }
    }
}
