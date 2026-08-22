package com.dopamine.ratt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

object Apps {

    private const val ICON_PX = 120

    @Volatile
    private var cache: List<AppEntry>? = null

    suspend fun load(context: Context): List<AppEntry> = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }

        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val self = context.packageName

        val entries = pm.queryIntentActivities(query, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != self }
            .map { resolved ->
                AppEntry(
                    packageName = resolved.activityInfo.packageName,
                    label = resolved.loadLabel(pm).toString(),
                    icon = resolved.loadIcon(pm).toImageBitmap(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()

        cache = entries
        entries
    }

    private fun Drawable.toImageBitmap(): ImageBitmap {
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, ICON_PX, ICON_PX)
        draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }
}
