package com.dopamine.ratt

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

/**
 * Watches which app just came to the front. Nothing else: window content
 * retrieval is off in the service config, so this only ever sees package names.
 */
class RattAccessibilityService : AccessibilityService() {

    /** Packages that have a launcher entry or are a home screen. */
    @Volatile
    private var launchable: Set<String> = emptySet()

    /**
     * Keyboards raise window changes of their own, and several of them have a
     * launcher entry for their settings. Letting one become "the app you were
     * in" would make putting the keyboard away look like a fresh arrival.
     */
    @Volatile
    private var keyboards: Set<String> = emptySet()

    private val packageWatcher = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshLaunchable()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshLaunchable()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this, packageWatcher, filter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        runCatching { unregisterReceiver(packageWatcher) }
        return super.onUnbind(intent)
    }

    /**
     * Home screens are in here as well as drawer entries. Without them, pressing
     * home would go unrecorded, the app you just backed out of would still look
     * like the app in front, and walking straight back into it would read as
     * never having left.
     */
    private fun refreshLaunchable() {
        val drawer = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        launchable = runCatching {
            val found = HashSet<String>()
            packageManager.queryIntentActivities(drawer, 0)
                .mapTo(found) { it.activityInfo.packageName }
            packageManager.queryIntentActivities(home, 0)
                .mapTo(found) { it.activityInfo.packageName }
            found
        }.getOrDefault(emptySet())

        keyboards = runCatching {
            getSystemService(InputMethodManager::class.java)
                ?.enabledInputMethodList
                ?.mapTo(HashSet()) { it.packageName }
                .orEmpty()
        }.getOrDefault(emptySet())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Our own interstitial must not overwrite the record of what was in front,
        // otherwise dismissing it would look like a fresh entry.
        if (pkg == packageName) return
        if (pkg in IGNORED) return
        if (pkg in keyboards) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        onEntry(pkg)
    }

    /** The whole-app half: did you just arrive somewhere you are watching? */
    private fun onEntry(pkg: String) {
        val watched = Watchlist.get(this)

        // Keyboards, toasts and overlays also raise window-state changes. Letting
        // one of those become "the app you were in" would make returning to the
        // app underneath look like a fresh entry and fire mid-session.
        if (pkg !in watched && pkg !in launchable) return

        // Whatever came to the front, a pass held for somewhere else is spent.
        Gate.sawForeground(pkg)

        // Comparing against the same package, not "any watched app", so that
        // switching straight from one watched app to another still triggers.
        val alreadyHere = lastForeground == pkg
        lastForeground = pkg

        if (pkg !in watched) return
        if (alreadyHere) return
        if (Gate.isOpen(pkg)) return

        interrupt(pkg)
    }

    private fun interrupt(key: String) {
        startActivity(
            Intent(this, InterstitialActivity::class.java)
                .putExtra(EXTRA_PACKAGE, key)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        const val EXTRA_PACKAGE = "triggered_by"

        /** Transient system windows that say nothing about which app you are in. */
        private val IGNORED = setOf(
            "com.android.systemui",
            "android",
        )

        /**
         * The last non-ours app seen in front, used to spot a fresh entry.
         *
         * On the companion rather than the instance so that it survives the
         * service being rebound, and so the interstitial can clear it.
         */
        @Volatile
        private var lastForeground: String? = null

        /**
         * Turning back at the sign is not a way in. Forgetting what was in front
         * means the next arrival counts as a fresh one and gets stopped again,
         * however the user got there.
         */
        fun forgetForeground() {
            lastForeground = null
        }
    }
}
