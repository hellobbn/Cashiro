package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.presentation.common.icons.IconResource

/**
 * A background component that tiles an icon without continuous redraws.
 * Decorative art stays still while data and interaction feedback remain responsive.
 */
@Composable
fun TiledScrollingIconBackground(
    iconResource: IconResource,
    modifier: Modifier = Modifier,
    opacity: Float = 0.05f,
    iconSize: Dp = 60.dp,
    rotation: Float = -20f,
    @Suppress("UNUSED_PARAMETER") animationDuration: Int = 15000
) {
    TiledScrollingIconBackground(
        iconResources = listOf(iconResource),
        modifier = modifier,
        opacity = opacity,
        iconSize = iconSize,
        rotation = rotation,
        animationDuration = animationDuration
    )
}

/**
 * A background component that tiles multiple icons without continuous redraws.
 * Icons alternate in both columns and rows. The historical animationDuration argument
 * is retained for source compatibility; decorative art no longer uses a frame clock.
 */
@Composable
fun TiledScrollingIconBackground(
    iconResources: List<IconResource>,
    modifier: Modifier = Modifier,
    opacity: Float = 0.05f,
    iconSize: Dp = 60.dp,
    rotation: Float = -20f,
    @Suppress("UNUSED_PARAMETER") animationDuration: Int = 15000
) {
    if (iconResources.isEmpty()) return

    // Resolve painters in composition; the canvas only draws again when its inputs change.
    val paintersWithTint = iconResources.map { iconResource ->
        val painter = when (iconResource) {
            is IconResource.DrawableResource -> painterResource(id = iconResource.resId)
            is IconResource.TintedResIcon -> painterResource(id = iconResource.resId)
            is IconResource.VectorIcon -> rememberVectorPainter(image = iconResource.icon)
        }

        val tint = when (iconResource) {
            is IconResource.TintedResIcon -> iconResource.tint
            is IconResource.VectorIcon -> iconResource.tint
            else -> Color.Unspecified
        }
        painter to tint
    }

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        val sizePx = iconSize.toPx()
        val spacing = sizePx * 0.4f
        val step = sizePx + spacing

        // Calculate how many items we need to cover the area
        // We add extra to handle rotation and overflow
        val columns = (size.width / step).toInt() + 4
        val rows = (size.height / step).toInt() + 4

        rotate(rotation) {
            for (col in -2..columns) {
                for (row in -2..rows) {
                    val x = col * step
                    val y = row * step
                    
                    // Select icon based on position to create alternating pattern
                    val index = (col + row).let { if (it < 0) -it else it } % iconResources.size
                    val (painter, tint) = paintersWithTint[index]

                    translate(left = x, top = y) {
                        val isBrandIcon = iconResources[index] is IconResource.DrawableResource
                        val drawSize = if (isBrandIcon) sizePx else sizePx * 0.7f
                        val offset = if (isBrandIcon) 0f else (sizePx - drawSize) / 2f

                        translate(left = offset, top = offset) {
                            with(painter) {
                                draw(
                                    size = Size(drawSize, drawSize),
                                    alpha = opacity,
                                    colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
