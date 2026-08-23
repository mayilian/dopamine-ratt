package com.dopamine.ratt

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dopamine.ratt.ui.Bone
import com.dopamine.ratt.ui.Display
import com.dopamine.ratt.ui.Ember
import com.dopamine.ratt.ui.Faint
import com.dopamine.ratt.ui.Ink
import com.dopamine.ratt.ui.Mono
import com.dopamine.ratt.ui.Muted

private enum class Step { UNLOCK, SERVICE }

/**
 * Android 13 and up hides accessibility for sideloaded apps behind a menu that
 * the app cannot open for you. On older versions there is nothing to unlock.
 */
private val steps: List<Step> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Step.UNLOCK)
    add(Step.SERVICE)
}

/**
 * What stands in front of the app until the accessibility service is on.
 *
 * There is no way past it, on purpose. Everything else in here is switches that
 * do nothing without the service, and an app that looks configured but never
 * interrupts anything is worse than one that plainly says it is not ready yet.
 *
 * It is not a first-run flow: it is shown whenever the service is off, so
 * switching the service off later brings it back rather than leaving a screen
 * of dead controls.
 */
@Composable
fun OnboardingScreen(
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    var step by remember { mutableStateOf(steps.first()) }
    val advance = { step = steps.getOrElse(steps.indexOf(step) + 1) { steps.last() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp),
    ) {
        Spacer(Modifier.height(30.dp))

        Text(
            text = "%02d / %02d".format(steps.indexOf(step) + 1, steps.size),
            color = Faint,
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
        )

        Spacer(Modifier.height(18.dp))

        when (step) {
            Step.UNLOCK -> {
                Heading("ALLOW\nRESTRICTED\nSETTINGS")
                Body("Android locks accessibility for sideloaded apps.\n\nIn the screen that opens: three dots, top right, then Allow restricted settings.")
                Spacer(Modifier.height(30.dp))
                Action(label = "OPEN APP INFO", emphasis = true, onClick = onOpenAppInfo)
                Spacer(Modifier.height(10.dp))
                Action(label = "DONE, NEXT", emphasis = false, onClick = advance)
            }

            Step.SERVICE -> {
                Heading("TURN\nIT ON")
                Body("Nothing is watched until this is on.")
                Spacer(Modifier.height(24.dp))
                LookFor()
                Spacer(Modifier.height(26.dp))
                Action(label = "ACCESSIBILITY SETTINGS", emphasis = true, onClick = onOpenAccessibility)
            }
        }

        Spacer(Modifier.height(30.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dot in steps) {
                Box(
                    Modifier
                        .size(if (dot == step) 7.dp else 5.dp)
                        .background(
                            color = if (dot == step) Ember else Faint.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

/**
 * The accessibility list is a list of apps, and the app you are looking for is
 * this one rather than the one you are trying to stay out of. Easy to get
 * backwards, and getting it backwards means switching on a service that does
 * nothing.
 */
@Composable
private fun LookFor() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Ember.copy(alpha = 0.45f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = "IN THE LIST, SWITCH ON",
            color = Faint,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "DOPAMINE RATT",
            color = Ember,
            fontFamily = Display,
            fontSize = 34.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Not Instagram. Not TikTok. This app.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        color = Bone,
        fontFamily = Display,
        fontSize = 54.sp,
        lineHeight = 50.sp,
        letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
}
