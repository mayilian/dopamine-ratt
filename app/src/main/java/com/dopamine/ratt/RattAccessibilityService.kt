package com.dopamine.ratt

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app just came to the front. Nothing else: window content
 * retrieval is off in the service config, so this only ever sees package names.
 */
class RattAccessibilityService : AccessibilityService() {

    /** The last non-ours app seen in front, used to spot a fresh entry. */
    private var lastForeground: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Our own interstitial must not overwrite the record of what was in front,
        // otherwise dismissing it would look like a fresh entry.
        if (pkg == packageName) return
        if (pkg in IGNORED) return

        // Comparing against the same package, not "any watched app", so that
        // switching straight from one watched app to another still triggers.
        val alreadyHere = lastForeground == pkg
        lastForeground = pkg

        if (pkg !in Watchlist.get(this)) return
        if (alreadyHere) return
        if (Gate.isOpen()) return

        startActivity(
            Intent(this, InterstitialActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        /** Transient system windows that say nothing about which app you are in. */
        private val IGNORED = setOf(
            "com.android.systemui",
            "android",
        )
    }
}
