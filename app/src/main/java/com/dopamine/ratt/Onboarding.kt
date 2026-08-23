package com.dopamine.ratt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * What stands in front of the app until the accessibility service is on.
 *
 * There is no way past it, on purpose. Everything else in here is switches that
 * do nothing without the service, and an app that looks configured but never
 * interrupts anything is worse than one that plainly says it is not ready yet.
 *
 * It is not a first run flow: it is shown whenever the service is off, so
 * switching the service off later brings it back rather than leaving a screen of
 * dead controls.
 */
@Composable
fun OnboardingScreen(
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp),
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            text = "TURN\nIT ON",
            color = Bone,
            fontFamily = Display,
            fontSize = 54.sp,
            lineHeight = 50.sp,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Nothing is watched until this is on.",
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.height(24.dp))

        LookFor()

        Spacer(Modifier.height(26.dp))

        Action(label = "ACCESSIBILITY SETTINGS", emphasis = true, onClick = onOpenAccessibility)

        Spacer(Modifier.height(22.dp))

        // The switch comes up greyed out for sideloaded apps until restricted
        // settings are unlocked, and that is a menu the app cannot open for you.
        // It is down here rather than on a page of its own: most people never
        // need it, and the ones who do are staring at a switch that will not move.
        Text(
            text = "SWITCH GREYED OUT?",
            color = Faint,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Android locks it for sideloaded apps. Open app info, then the three dots at the top right, then Allow restricted settings.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(14.dp))

        Action(label = "OPEN APP INFO", emphasis = false, onClick = onOpenAppInfo)

        Spacer(Modifier.height(24.dp))
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
