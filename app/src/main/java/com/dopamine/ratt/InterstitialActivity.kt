package com.dopamine.ratt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dopamine.ratt.ui.Bone
import com.dopamine.ratt.ui.Ember
import com.dopamine.ratt.ui.Ink
import com.dopamine.ratt.ui.Mono
import com.dopamine.ratt.ui.RattTheme
import kotlinx.coroutines.delay
import android.graphics.Color as AndroidColor

/** How long the user is held before the way through is offered. */
private const val HOLD_MILLIS = 4000

/**
 * If nobody touches anything, stop burning the display on a glowing sign and
 * send them home. Without this the animations run until the screen times out.
 */
private const val ABANDON_MILLIS = 45_000L

class InterstitialActivity : ComponentActivity() {

    /** Which watched app, or which surface of one, put us here. */
    private val triggeredBy: String?
        get() = intent?.getStringExtra(RattAccessibilityService.EXTRA_PACKAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            RattTheme {
                InterstitialScreen(
                    surface = triggeredBy?.let { Surfaces.byKey(it)?.label },
                    onLeave = ::leave,
                    onContinue = ::goThrough,
                )
            }
        }
    }

    /**
     * Backing out, or walking away and letting it time out. Neither is a way in:
     * the pass is dropped, and the service is told to forget what was in front
     * so that coming back here counts as a fresh arrival and gets stopped again.
     */
    private fun leave() {
        Gate.close()
        RattAccessibilityService.forgetForeground()
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    /** The only thing in the app that opens a pass, and it lasts one visit. */
    private fun goThrough() {
        triggeredBy?.let { Gate.open(it) }
        finish()
    }
}

@Composable
private fun InterstitialScreen(
    surface: String?,
    onLeave: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current

    var unlocked by remember { mutableStateOf(false) }
    val hold = remember { Animatable(0f) }

    LaunchedEffect(Unit) { knock(context) }
    LaunchedEffect(Unit) {
        hold.animateTo(1f, tween(HOLD_MILLIS, easing = FastOutSlowInEasing))
        unlocked = true
    }
    LaunchedEffect(Unit) {
        delay(ABANDON_MILLIS)
        onLeave()
    }

    val revealAlpha by animateFloatAsState(
        targetValue = if (unlocked) 1f else 0f,
        animationSpec = tween(700),
        label = "reveal",
    )

    BackHandler(enabled = true) { onLeave() }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {

        NeonSign()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 30.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Named only when it was a surface rather than the whole app, so the
            // stop does not look like it fired at random halfway through a session.
            if (surface != null) {
                Text(
                    text = surface,
                    color = Bone.copy(alpha = 0.8f),
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    letterSpacing = 5.sp,
                )
                Spacer(Modifier.height(22.dp))
            }

            // The hold, as a line rather than a word.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(2.dp)
                    .background(Bone.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hold.value)
                        .fillMaxHeight()
                        .background(Ember),
                )
            }

            Spacer(Modifier.height(34.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tap(
                    label = "BACK",
                    color = Bone.copy(alpha = 0.72f),
                    onClick = onLeave,
                )

                Spacer(Modifier.width(28.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Bone.copy(alpha = 0.18f)),
                )
                Spacer(Modifier.width(28.dp))

                // Bright rather than dim: it is hidden until the hold is over,
                // and once it is offered it has to be readable against the wash.
                Tap(
                    label = "ENTER",
                    color = Bone,
                    enabled = unlocked,
                    modifier = Modifier.graphicsLayer { alpha = revealAlpha },
                    onClick = onContinue,
                )
            }
        }
    }
}

@Composable
private fun Tap(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Text(
        text = label,
        color = color,
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 4.sp,
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp, horizontal = 10.dp),
    )
}
