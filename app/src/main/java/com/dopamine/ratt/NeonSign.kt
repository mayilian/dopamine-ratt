package com.dopamine.ratt

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random

private val NeonRed = Color(0xFFFF1F3D)
private val NeonPink = Color(0xFFFF2BD1)
private val NeonCyan = Color(0xFF19E8FF)

/**
 * The centrepiece: bloom, chromatic fringe, breathing, and the occasional
 * bad-ballast flicker.
 *
 * By default this lights up the emblem drawn for the app. If an image has been
 * dropped into the app's external files directory as emblem.png/jpg/jpeg/webp,
 * that gets lit instead, so the artwork is whatever the owner of the phone puts
 * there rather than anything baked into the APK.
 */
@Composable
fun NeonSign(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val custom = remember { loadEmblem(context) }

    val entrance = remember { Animatable(0.72f) }
    val opacity = remember { Animatable(0f) }
    LaunchedEffect(Unit) { opacity.animateTo(1f, tween(380)) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow))
    }

    val loop = rememberInfiniteTransition(label = "neon")

    val breath by loop.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    val bloom by loop.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bloom",
    )

    val drift by loop.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    // Neon never burns perfectly steady.
    val flicker = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(1400, 4200))
            flicker.snapTo(0.42f)
            delay(42)
            flicker.snapTo(1f)
            delay(58)
            flicker.snapTo(0.68f)
            delay(34)
            flicker.snapTo(1f)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.34f * entrance.value * breath
            val alpha = opacity.value * flicker.value

            drawBloom(centre, radius, bloom * alpha)

            if (custom != null) {
                drawChromaticGhost(custom, centre, radius, drift, alpha)
                drawEmblem(custom, centre, radius, alpha)
            }
        }

        if (custom == null) {
            val scale = entrance.value * breath
            val alpha = opacity.value * flicker.value
            val shift = (drift * 7f).dp
            val painter = painterResource(R.drawable.ic_neon_rat)

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().offset(x = -shift),
                    colorFilter = ColorFilter.tint(NeonCyan),
                    alpha = 0.55f,
                )
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().offset(x = shift),
                    colorFilter = ColorFilter.tint(NeonPink),
                    alpha = 0.55f,
                )
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Soft halo built from stacked translucent circles. */
private fun DrawScope.drawBloom(centre: Offset, radius: Float, intensity: Float) {
    for (step in 10 downTo 1) {
        drawCircle(
            color = NeonRed.copy(alpha = 0.045f * intensity),
            radius = radius * (1f + step * 0.075f),
            center = centre,
        )
    }
    for (step in 5 downTo 1) {
        drawCircle(
            color = NeonPink.copy(alpha = 0.05f * intensity),
            radius = radius * (1f + step * 0.035f),
            center = centre,
        )
    }
}

private fun emblemRect(
    image: ImageBitmap,
    centre: Offset,
    radius: Float,
): Pair<IntOffset, IntSize> {
    val box = radius * 2f
    val scale = minOf(box / image.width, box / image.height)
    val w = (image.width * scale).roundToInt()
    val h = (image.height * scale).roundToInt()
    val offset = IntOffset((centre.x - w / 2f).roundToInt(), (centre.y - h / 2f).roundToInt())
    return offset to IntSize(w, h)
}

private fun DrawScope.drawEmblem(
    image: ImageBitmap,
    centre: Offset,
    radius: Float,
    alpha: Float,
) {
    val (offset, target) = emblemRect(image, centre, radius)
    drawImage(image = image, dstOffset = offset, dstSize = target, alpha = alpha)
}

/** Two offset copies underneath, so the edges bleed cyan and magenta. */
private fun DrawScope.drawChromaticGhost(
    image: ImageBitmap,
    centre: Offset,
    radius: Float,
    drift: Float,
    alpha: Float,
) {
    val shift = (radius * 0.03f * drift).roundToInt()
    val (offset, target) = emblemRect(image, centre, radius)
    drawImage(
        image = image,
        dstOffset = IntOffset(offset.x - shift, offset.y),
        dstSize = target,
        alpha = 0.5f * alpha,
        colorFilter = ColorFilter.tint(NeonCyan),
    )
    drawImage(
        image = image,
        dstOffset = IntOffset(offset.x + shift, offset.y),
        dstSize = target,
        alpha = 0.5f * alpha,
        colorFilter = ColorFilter.tint(NeonPink),
    )
}

/** Whatever the phone's owner has chosen to put in the slot, if anything. */
private fun loadEmblem(context: Context): ImageBitmap? {
    val dir = context.getExternalFilesDir(null) ?: return null
    val file = listOf("emblem.png", "emblem.jpg", "emblem.jpeg", "emblem.webp")
        .map { File(dir, it) }
        .firstOrNull { it.exists() }
        ?: return null

    return runCatching {
        BitmapFactory.decodeFile(file.path)?.asImageBitmap()
    }.getOrNull()
}
