package com.dopamine.ratt

/**
 * A part of an app rather than the whole app. Instagram is the one that needs
 * this: plenty of people want the messages and the posts and specifically do
 * not want the endless feeds.
 *
 * A surface is off until it is turned on. It costs more to watch for one than
 * to watch for a package name, so nothing here runs until you ask for it.
 */
data class Surface(
    val key: String,
    val packageName: String,
    val label: String,
    /** Substrings of view ids. Nothing else about the view is read. */
    val markers: List<String>,
    /** Screens that announce themselves by class name, which costs nothing to check. */
    val classes: List<String>,
)

object Surfaces {

    const val INSTAGRAM = "com.instagram.android"

    /**
     * The markers are Instagram's own internal names, which are not the names on
     * the buttons: "clips" is Reels, and "reel" is Stories. They are matched as
     * substrings so a version bump that appends a suffix does not break them,
     * and they are specific enough to miss the Stories tray and the Reels tab
     * button, both of which sit on the home feed.
     *
     * Stories is first because it is checked first. The story viewer opens over
     * whatever you were looking at, so on a story opened from the Reels tab both
     * sets of ids are on screen at once, and the one on top is the answer.
     */
    val ALL = listOf(
        Surface(
            key = "$INSTAGRAM#stories",
            packageName = INSTAGRAM,
            label = "STORIES",
            markers = listOf("reel_viewer", "reel_view_group", "story_viewer"),
            classes = listOf("ReelViewer", "StoryViewer"),
        ),
        Surface(
            key = "$INSTAGRAM#reels",
            packageName = INSTAGRAM,
            label = "REELS",
            markers = listOf("clips_viewer", "clips_video", "clips_swipe_refresh"),
            classes = listOf("ClipsViewer"),
        ),
    )

    fun of(packageName: String): List<Surface> = ALL.filter { it.packageName == packageName }

    fun byKey(key: String): Surface? = ALL.firstOrNull { it.key == key }
}
