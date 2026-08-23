package com.dopamine.ratt

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.random.Random

private val Signal = Color(0xFFD62C2C)
private val SignalDeep = Color(0xFFA31D1D)
private val InkDeep = Color(0xFF1E1B1B)
private val Paper = Color(0xFFF0E8D8)
private val Acid = Color(0xFFFFD54F)

/**
 * The pixel values below were written against a sign 448px across, so they are
 * scaled by that ratio rather than used raw.
 */
private const val REFERENCE_PX = 448f

private const val BREATHE_MILLIS = 5000
private const val RING_MILLIS = 3000
private const val GLITCH_MILLIS = 6000
private const val RISE_MILLIS = 6000

private const val BREATHE_TILT = 1.2f
private const val BREATHE_SWELL = 1.045f

/** The rings start inside the sign and end well outside it. */
private const val RING_TO_SIGN = 62f / 72f

private const val MOTE_COUNT = 9

/**
 * The centrepiece: the sign sits in a radial wash, breathes, throws off pulse
 * rings, and every six seconds catches a short glitch.
 *
 * By default this lights up the rat sign shipped with the app. If an image has
 * been dropped into the app's external files directory as emblem.png/jpg/jpeg/webp,
 * that gets lit instead, so the artwork can be whatever the owner of the phone
 * puts there.
 */
@Composable
fun NeonSign(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val custom = remember { loadEmblem(context) }
    val emblem = custom ?: ImageBitmap.imageResource(R.drawable.emblem_ratt)

    val entrance = remember { Animatable(0.72f) }
    val opacity = remember { Animatable(0f) }
    LaunchedEffect(Unit) { opacity.animateTo(1f, tween(380)) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow))
    }

    val loop = rememberInfiniteTransition(label = "sign")

    val breath by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BREATHE_MILLIS / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    // These three run linear and are eased where they are read, so a phase
    // offset stays a phase offset rather than being bent by the easing.
    val ring by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RING_MILLIS, easing = LinearEasing)),
        label = "ring",
    )

    val glitch by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GLITCH_MILLIS, easing = LinearEasing)),
        label = "glitch",
    )

    val rise by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RISE_MILLIS, easing = LinearEasing)),
        label = "rise",
    )

    val motes = remember {
        val seeded = Random(19)
        List(MOTE_COUNT) { Mote(x = seeded.nextFloat(), phase = seeded.nextFloat()) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawWash()

        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.36f * entrance.value
        val unit = radius * 2f / REFERENCE_PX
        val alpha = opacity.value

        drawRings(centre, radius, ring, unit, alpha)
        drawMotes(motes, rise, unit, alpha)

        withTransform({ transform(signTransform(centre, breath, joltAt(glitch), unit)) }) {
            drawGlow(centre, radius, unit, alpha)
            drawEmblem(emblem, centre, radius, alpha)
        }
    }
}

/** Red at the middle, falling away to near black at the corners. */
private fun DrawScope.drawWash() {
    val centre = Offset(size.width / 2f, size.height * 0.45f)
    val corners = listOf(
        Offset.Zero,
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height),
    )
    val reach = corners.maxOf { (it - centre).getDistance() }

    drawRect(
        brush = Brush.radialGradient(
            0f to Signal,
            0.55f to SignalDeep,
            1f to InkDeep,
            center = centre,
            radius = reach,
        ),
    )
}

/**
 * Two rings leaving the sign, the second half a cycle behind the first, so
 * there is always one on its way out.
 */
private fun DrawScope.drawRings(
    centre: Offset,
    radius: Float,
    clock: Float,
    unit: Float,
    alpha: Float,
) {
    drawRing(centre, radius, clock, Paper, 0.5f, unit, alpha)
    drawRing(centre, radius, (clock + 0.5f) % 1f, Acid, 0.4f, unit, alpha)
}

private fun DrawScope.drawRing(
    centre: Offset,
    radius: Float,
    clock: Float,
    color: Color,
    weight: Float,
    unit: Float,
    alpha: Float,
) {
    val t = LinearOutSlowInEasing.transform(clock)
    val scale = lerp(0.75f, 1.9f, t)
    val fade = lerp(0.75f, 0f, t)
    if (fade <= 0f) return

    drawCircle(
        color = color.copy(alpha = weight * fade * alpha),
        radius = radius * RING_TO_SIGN * scale,
        center = centre,
        style = Stroke(width = 6f * unit * scale),
    )
}

/**
 * Sparks off the bottom of the frame, drifting up and burning out. They start
 * above the row of labels so they read as embers rather than as dirt on the
 * buttons.
 */
private fun DrawScope.drawMotes(motes: List<Mote>, clock: Float, unit: Float, alpha: Float) {
    val floor = size.height - 150f * unit

    for (mote in motes) {
        val t = LinearOutSlowInEasing.transform((clock + mote.phase) % 1f)
        val fade = if (t < 0.2f) t / 0.2f else 1f - (t - 0.2f) / 0.8f

        drawCircle(
            color = Acid.copy(alpha = fade * alpha),
            radius = 4f * unit * lerp(0.9f, 1.1f, t),
            center = Offset(mote.x * size.width, floor + lerp(20f, -260f, t) * unit),
        )
    }
}

private class Mote(val x: Float, val phase: Float)

/**
 * The breathing swell and tilt, plus whatever the glitch is doing this frame,
 * as one matrix pivoted on the sign.
 */
private fun signTransform(centre: Offset, breath: Float, jolt: Jolt, unit: Float): Matrix {
    val matrix = Matrix()
    matrix.translate(jolt.dx * unit, jolt.dy * unit)
    matrix.translate(centre.x, centre.y)
    matrix.rotateZ(lerp(-BREATHE_TILT, BREATHE_TILT, breath))

    val swell = lerp(1f, BREATHE_SWELL, breath)
    matrix.scale(swell, swell)

    if (jolt.skew != 0f) matrix *= skewX(jolt.skew)
    matrix.translate(-centre.x, -centre.y)
    return matrix
}

private fun skewX(degrees: Float): Matrix {
    val matrix = Matrix()
    matrix.values[Matrix.SkewX] = tan(degrees * PI / 180f).toFloat()
    return matrix
}

private class Jolt(val dx: Float, val dy: Float, val skew: Float)

private val Steady = Jolt(0f, 0f, 0f)

/**
 * Still for most of the cycle, then three hard frames near the end of it. The
 * stops are where the sign is at each point of the cycle; between them it is
 * eased, which is what keeps the jolt from reading as a jump cut.
 */
private val JOLTS = listOf(
    0.92f to Steady,
    0.94f to Jolt(-4f, 2f, -6f),
    0.96f to Jolt(5f, -2f, 5f),
    0.98f to Jolt(-2f, 1f, -2f),
    1f to Steady,
)

private fun joltAt(clock: Float): Jolt {
    if (clock < JOLTS.first().first) return Steady

    for (i in 0 until JOLTS.lastIndex) {
        val (at, from) = JOLTS[i]
        val (next, to) = JOLTS[i + 1]
        if (clock > next) continue

        val t = FastOutSlowInEasing.transform((clock - at) / (next - at))
        return Jolt(
            dx = lerp(from.dx, to.dx, t),
            dy = lerp(from.dy, to.dy, t),
            skew = lerp(from.skew, to.skew, t),
        )
    }
    return Steady
}

/** The red cast the sign throws below itself, stacked rather than blurred. */
private fun DrawScope.drawGlow(centre: Offset, radius: Float, unit: Float, alpha: Float) {
    val seat = Offset(centre.x, centre.y + 30f * unit)
    for (step in 12 downTo 1) {
        drawCircle(
            color = Signal.copy(alpha = 0.05f * alpha),
            radius = radius * (0.91f + step * 0.033f),
            center = seat,
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

/** Nothing on screen needs more than this, and it caps what a huge file can cost. */
private const val MAX_EMBLEM_PX = 1024

/**
 * Whatever the phone's owner has chosen to put in the slot, if anything.
 *
 * The header is read first so an oversized image is downsampled during decode
 * rather than being fully expanded in memory.
 */
private fun loadEmblem(context: Context): ImageBitmap? {
    val dir = context.getExternalFilesDir(null) ?: return null
    val file = listOf("emblem.png", "emblem.jpg", "emblem.jpeg", "emblem.webp")
        .map { File(dir, it) }
        .firstOrNull { it.exists() }
        ?: return null

    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (
            bounds.outWidth / sample > MAX_EMBLEM_PX ||
            bounds.outHeight / sample > MAX_EMBLEM_PX
        ) {
            sample *= 2
        }

        BitmapFactory
            .decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?.asImageBitmap()
    }.getOrNull()
}
