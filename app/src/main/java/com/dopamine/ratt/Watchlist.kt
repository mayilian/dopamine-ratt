package com.dopamine.ratt

import android.content.Context

/**
 * Which apps get intercepted. Read by the accessibility service on every
 * window change, written by the picker.
 */
object Watchlist {

    private const val FILE = "ratt"
    private const val KEY = "watched"

    /** Pre-ticked on first run if installed, so the app does something out of the box. */
    val SUGGESTED = listOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.facebook.katana",
        "com.google.android.youtube",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.instagram.barcelona",
        "com.linkedin.android",
        "com.pinterest",
        "com.netflix.mediaclient",
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * Held in memory because the accessibility service asks for this on every
     * window change device-wide. Re-reading prefs and allocating a fresh set on
     * that path was the hottest thing in the app.
     *
     * The service and the picker share a process, so writing through here keeps
     * the two in step.
     */
    @Volatile
    private var cached: Set<String>? = null

    fun get(context: Context): Set<String> {
        cached?.let { return it }
        val loaded = prefs(context).getStringSet(KEY, emptySet()).orEmpty().toSet()
        cached = loaded
        return loaded
    }

    fun set(context: Context, packages: Set<String>) {
        val next = packages.toSet()
        prefs(context).edit().putStringSet(KEY, next).apply()
        cached = next
    }

    fun toggle(context: Context, packageName: String): Set<String> {
        val current = get(context)
        val next = if (packageName in current) current - packageName else current + packageName
        set(context, next)
        return next
    }

    /** Ticks the suggested apps once, the first time the picker is opened. */
    fun seedIfEmpty(context: Context, installed: Set<String>) {
        val prefs = prefs(context)
        if (prefs.getBoolean("seeded", false)) return
        prefs.edit().putBoolean("seeded", true).apply()
        if (get(context).isNotEmpty()) return
        set(context, SUGGESTED.filter { it in installed }.toSet())
    }
}
