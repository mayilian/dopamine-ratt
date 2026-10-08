package com.dopamine.ratt

import android.content.Context

/**
 * Whether the accessibility disclosure has been read and accepted.
 *
 * Play does not treat a disclosure as satisfied by being on screen. It has to be
 * accepted by a deliberate action of its own, ahead of the grant, and that is
 * what this records.
 *
 * Remembering it is also what lets the switch instructions stand alone
 * afterwards. The onboarding comes back every time the service is off, and
 * making someone re-read the whole disclosure because they toggled the service
 * would be nagging rather than disclosing.
 */
object Consent {

    private const val FILE = "ratt"
    private const val KEY = "disclosure_accepted"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun accepted(context: Context): Boolean = prefs(context).getBoolean(KEY, false)

    fun accept(context: Context) {
        prefs(context).edit().putBoolean(KEY, true).apply()
    }
}
