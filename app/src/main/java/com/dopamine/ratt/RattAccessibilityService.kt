package com.dopamine.ratt

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

/**
 * Watches which app just came to the front, and, only if you have asked for it,
 * whether the app in front has landed on a surface you are trying to stay out
 * of.
 *
 * The first job needs nothing but the package name on the event. The second one
 * reads view ids out of the window, and is the only reason this service asks to
 * retrieve window content at all. With no surfaces armed the service does not
 * even subscribe to the events that would let it look.
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

    /** Rate limit for the only expensive thing in here. */
    private var lastScan = 0L

    private val packageWatcher = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshLaunchable()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshLaunchable()
        syncEventTypes()
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

    /**
     * Surface watching needs two things that watching for a package name does
     * not: content events, which arrive in their thousands, and the views an app
     * has marked as not worth announcing, which is where the ids we match on
     * live. Neither is asked for until a surface is armed, and both are given
     * back when the last one is switched off.
     */
    private fun syncEventTypes() {
        val info = serviceInfo ?: return
        val armed = Watchlist.surfaces(this).isNotEmpty()

        val events = if (armed) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        } else {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }

        // Ids are not reported unless asked for, and the containers they sit on
        // are the ones an app has marked as not worth announcing.
        val extra = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        val flags = if (armed) info.flags or extra else info.flags and extra.inv()

        if (info.eventTypes != events || info.flags != flags) {
            info.eventTypes = events
            info.flags = flags
            serviceInfo = info
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Our own interstitial must not overwrite the record of what was in front,
        // otherwise dismissing it would look like a fresh entry.
        if (pkg == packageName) return
        if (pkg in IGNORED) return
        if (pkg in keyboards) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                syncEventTypes()
                onEntry(pkg)
                onSurface(pkg, event.className?.toString())
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> onSurface(pkg, null)
        }
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

    /**
     * The inside-the-app half. Class names come free with the event; the node
     * walk below is the fallback, and it is why this runs on a leash.
     */
    private fun onSurface(pkg: String, className: String?) {
        val armed = Surfaces.of(pkg).filter { it.key in Watchlist.surfaces(this) }
        if (armed.isEmpty()) return

        val undecided = armed.filterNot { Gate.isOpen(it.key) }
        if (undecided.isEmpty()) return

        if (className != null) {
            val named = undecided.firstOrNull { surface ->
                surface.classes.any { className.contains(it, ignoreCase = true) }
            }
            if (named != null) {
                interrupt(named.key)
                return
            }
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastScan < SCAN_INTERVAL_MILLIS) return
        lastScan = now

        val root = rootInActiveWindow ?: return
        val onScreen = visibleIds(root)

        // A viewer fills the screen. A tab button that leads to one, or a
        // thumbnail of one, does not, and is not where you are.
        val found = undecided.firstOrNull { surface ->
            onScreen.any { (id, height) ->
                height >= minimumViewerHeight() && surface.markers.any { id.contains(it) }
            }
        } ?: return

        interrupt(found.key)
    }

    /**
     * The id of everything on screen, with how tall it is.
     *
     * Breadth first, because these ids live on containers near the root. Nodes
     * lying off the display are dropped along with everything under them:
     * Instagram keeps the next tab built and waiting out to the side, and
     * without that the Reels page is found while you are still on the feed.
     *
     * Only the id is read, never the text on it, and the walk gives up rather
     * than run long.
     */
    private fun visibleIds(root: AccessibilityNodeInfo): List<Pair<String, Int>> {
        val found = ArrayList<Pair<String, Int>>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val bounds = Rect()
        val screen = Rect(
            0,
            0,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels,
        )

        queue.add(root)
        var seen = 0

        while (queue.isNotEmpty() && seen < NODE_BUDGET) {
            val node = queue.removeFirst()
            seen++

            node.getBoundsInScreen(bounds)
            val sized = bounds.width() > 0 && bounds.height() > 0
            if (sized && !Rect.intersects(bounds, screen)) continue

            node.viewIdResourceName?.let { found.add(it to bounds.height()) }
            for (i in 0 until node.childCount) {
                queue.add(node.getChild(i) ?: continue)
            }
        }
        return found
    }

    /**
     * Breadth first so the containers near the root, which is where these ids
     * live, are reached in the first handful of nodes. Only the view id is read,
     * never the text on it, and the walk gives up rather than run long.
     *
     * Nodes that are not on screen are skipped along with everything under
     * them. Instagram keeps the next tab built and waiting off to the side, so
     * without this the Reels page is found while you are still on the feed and
     * the stop fires at somebody who never went there.
     */
    private fun findSurface(root: AccessibilityNodeInfo, candidates: List<Surface>): Surface? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        val bounds = Rect()

        while (queue.isNotEmpty() && seen < NODE_BUDGET) {
            val node = queue.removeFirst()
            seen++

            if (!node.isVisibleToUser) continue

            val id = node.viewIdResourceName
            if (id != null) {
                val hit = candidates.firstOrNull { surface ->
                    surface.markers.any { id.contains(it) }
                }
                // A viewer fills the screen. A thumbnail of one, or a tab button
                // that leads to one, does not, and is not where you are.
                if (hit != null) {
                    node.getBoundsInScreen(bounds)
                    if (bounds.height() >= minimumViewerHeight()) return hit
                }
            }

            for (i in 0 until node.childCount) {
                queue.add(node.getChild(i) ?: continue)
            }
        }
        return null
    }

    /** Half the display, which no tab strip or preview tile ever reaches. */
    private fun minimumViewerHeight(): Int =
        resources.displayMetrics.heightPixels / 2

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

        private const val SCAN_INTERVAL_MILLIS = 450L
        private const val NODE_BUDGET = 400

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
