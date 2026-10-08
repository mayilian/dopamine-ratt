package com.dopamine.ratt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * Two screens rather than one. The first asks you to hand an app an
 * accessibility service and says plainly what that gets it; the second is the
 * mechanical business of finding a switch in a system list. Those are different
 * jobs, and running them together — which is what this used to do — meant the
 * disclosure was something you read past on the way to the button.
 *
 * Play wants the disclosure accepted rather than merely shown: a deliberate
 * action, in the app, ahead of the grant. Splitting the screens is what gives it
 * one, and it is the lighter screen either way.
 *
 * It is not a first run flow: it is shown whenever the service is off, so
 * switching the service off later brings it back rather than leaving a screen of
 * dead controls. The acceptance is remembered though, so coming back lands on
 * the switch instructions instead of on the disclosure a second time.
 */
@Composable
fun OnboardingScreen(
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onDecline: () -> Unit,
) {
    val context = LocalContext.current
    var showingDisclosure by remember { mutableStateOf(!Consent.accepted(context)) }

    if (showingDisclosure) {
        DisclosureScreen(
            onAccept = {
                Consent.accept(context)
                showingDisclosure = false
            },
            onDecline = onDecline,
        )
    } else {
        SwitchOnScreen(
            onOpenAppInfo = onOpenAppInfo,
            onOpenAccessibility = onOpenAccessibility,
            onReread = { showingDisclosure = true },
        )
    }
}

/**
 * What the accessibility service is for, said before it is switched on rather
 * than after.
 *
 * Handing an app an accessibility service is the largest thing a person is asked
 * to do here, and they should be told what it reads before they do it, not left
 * to find it in a policy afterwards.
 *
 * The three lines at the bottom are the ones worth being specific about. "No
 * network access" in particular is not a promise about conduct, it is a fact
 * about the manifest: the app never asks for the internet permission, so the
 * claim can be checked against the permission list on the store page rather than
 * taken on trust.
 */
@Composable
private fun DisclosureScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    // Back must never read as agreement, so it lands on the same refusal the
    // DECLINE button does rather than falling through to the screen behind.
    BackHandler(onBack = onDecline)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp),
    ) {
        Spacer(Modifier.height(36.dp))

        Text(
            text = "ACCESSIBILITY",
            color = Bone,
            fontFamily = Display,
            fontSize = 50.sp,
            lineHeight = 46.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = "PERMISSION",
            color = Ember,
            fontFamily = Display,
            fontSize = 50.sp,
            lineHeight = 46.sp,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Dopamine Ratt uses Android's accessibility service to see which app has come to the front, so it can get there before you do.",
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = "WHAT IT READS",
            color = Faint,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "The name of the app in front. Never the contents of your screen, and never what you type.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "AND IT ALWAYS STAYS",
            color = Faint,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
        )

        Spacer(Modifier.height(16.dp))

        Guarantee("PRIVATE", "No account, no analytics, no tracking.")
        Spacer(Modifier.height(14.dp))
        Guarantee("OFFLINE", "The app has no network access to send it over.")
        Spacer(Modifier.height(14.dp))
        Guarantee("ON-DEVICE", "Your watchlist is stored on this phone only.")

        Spacer(Modifier.height(34.dp))

        // Two buttons, not one. Play does not accept a single acknowledging
        // button as consent: there has to be a refusal on screen, the same size
        // and as easy to hit as the agreement, and it has to do something.
        Action(label = "ACCEPT", emphasis = true, onClick = onAccept)

        Spacer(Modifier.height(10.dp))

        Action(label = "DECLINE", emphasis = false, onClick = onDecline)

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Declining closes the app. Nothing is watched until you accept and turn the service on yourself.",
            color = Faint,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/** One promise, said in a word and then in a sentence, so the column scans first. */
@Composable
private fun Guarantee(label: String, line: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .padding(top = 5.dp)
                .size(7.dp)
                .background(Ember, CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = Bone,
                fontFamily = Mono,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = line,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

/**
 * The mechanical half: the switch is in a system list this app cannot reach into,
 * so all that can be done is to name the row to look for and open the list.
 */
@Composable
private fun SwitchOnScreen(
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onReread: () -> Unit,
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

        Spacer(Modifier.height(26.dp))

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

        // The disclosure is a screen you pass through once. This is the way back
        // to it, for anyone who wants to read it again without turning the
        // service off to get there.
        Text(
            text = "← WHAT IT READS",
            color = Ember,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onReread,
                )
                .padding(vertical = 8.dp),
        )

        Spacer(Modifier.height(16.dp))
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
