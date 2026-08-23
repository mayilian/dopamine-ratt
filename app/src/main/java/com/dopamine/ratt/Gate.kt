package com.dopamine.ratt

/**
 * The one way in, and it lasts exactly as long as the visit it was opened for.
 *
 * Tapping ENTER opens this for the app, or the surface of an app, you were
 * stopped at. Going somewhere else closes it. There is no clock on it and no
 * way to earn one: every fresh arrival is stopped, which is the entire point.
 *
 * Held in memory rather than in prefs on purpose. The service and the
 * interstitial share a process, and a pass that outlived the process would be a
 * pass that outlived the decision that opened it.
 */
object Gate {

    @Volatile
    private var granted: String? = null

    /** Called from the interstitial, and nowhere else. */
    fun open(key: String) {
        granted = key
    }

    fun isOpen(key: String): Boolean = granted == key

    fun close() {
        granted = null
    }

    /**
     * Something came to the front. A pass only ever covers its own app, so
     * anything else arriving is the visit ending.
     */
    fun sawForeground(packageName: String) {
        val held = granted ?: return
        if (held.substringBefore(SURFACE_SEPARATOR) != packageName) granted = null
    }

    const val SURFACE_SEPARATOR = '#'
}
