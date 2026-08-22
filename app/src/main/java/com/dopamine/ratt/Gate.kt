package com.dopamine.ratt

import android.os.SystemClock

/**
 * Shared between the accessibility service and the interstitial, both of which
 * live in the same process.
 *
 * When the user decides to go in anyway, the gate opens briefly so the target
 * app can come to the front without immediately triggering another rat.
 */
object Gate {

    @Volatile
    private var openUntilElapsed = 0L

    fun openFor(millis: Long) {
        openUntilElapsed = SystemClock.elapsedRealtime() + millis
    }

    fun close() {
        openUntilElapsed = 0L
    }

    fun isOpen(): Boolean = SystemClock.elapsedRealtime() < openUntilElapsed
}
