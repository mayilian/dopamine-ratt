package com.dopamine.ratt.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.dopamine.ratt.R

/**
 * Near black with a violet bias, so the edge glow has something to sit in.
 *
 * The three greys are steps down in emphasis, not steps down into the
 * background: against Ink they run 17:1, 7.9:1 and 5.7:1, so the dimmest label
 * in the app still clears the 4.5:1 that body text is held to.
 */
val Ink = Color(0xFF08070A)
val Bone = Color(0xFFF2EEE9)
val Muted = Color(0xFFA5A1AC)
val Faint = Color(0xFF8A8694)

/** Mystic. */
val Violet = Color(0xFF7B2CFF)

/** Industrial. */
val Ember = Color(0xFFFF4D1C)

/** Tall condensed caps: poster type, reads industrial at size. */
val Display = FontFamily(Font(R.font.bebas_neue))

/** Micro type is monospace so the small labels read as instrumentation. */
val Mono = FontFamily.Monospace

private val Scheme = darkColorScheme(
    primary = Ember,
    onPrimary = Ink,
    background = Ink,
    onBackground = Bone,
    surface = Ink,
    onSurface = Bone,
)

@Composable
fun RattTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
