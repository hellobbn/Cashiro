package com.ritesh.cashiro.presentation.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.common.icons.IconResource
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuietDecorationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun lightDecorationsStayStill() = exercise(false)
    @Test fun darkDecorationsStayStill() = exercise(true)

    private fun exercise(dark: Boolean) {
        rule.setContent {
            CashiroTheme(dynamicColor = false, darkTheme = dark) {
                Surface {
                Column(Modifier.size(320.dp, 400.dp).padding(top = 56.dp).testTag("decoration")) {
                    BudgetAnimatedGradientMeshCard(budgetColor = Color.Blue) {
                        Box(Modifier.height(160.dp)) { Text("Budget: 50% remaining", modifier = Modifier.padding(16.dp)) }
                    }
                    Box(Modifier.height(160.dp)) {
                        TiledScrollingIconBackground(
                            iconResource = IconResource.DrawableResource(R.drawable.ic_launcher_foreground),
                            opacity = 0.2f
                        )
                    }
                    // Empty art inputs are a valid no-op.
                    TiledScrollingIconBackground(iconResources = emptyList())
                }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        val before = rule.onNodeWithTag("decoration").captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(8_000)
        val after = rule.onNodeWithTag("decoration").captureToImage().asAndroidBitmap()
        assertTrue("Decorative pixels must not animate while idle", before.sameAs(after))
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "performance-review").apply { mkdirs() }
        File(dir, if (dark) "dark.png" else "light.png").outputStream().use {
            after.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
