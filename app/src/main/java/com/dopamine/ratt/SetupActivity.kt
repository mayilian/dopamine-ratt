package com.dopamine.ratt

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dopamine.ratt.ui.Bone
import com.dopamine.ratt.ui.Display
import com.dopamine.ratt.ui.Ember
import com.dopamine.ratt.ui.Faint
import com.dopamine.ratt.ui.Ink
import com.dopamine.ratt.ui.Mono
import com.dopamine.ratt.ui.Muted
import com.dopamine.ratt.ui.RattTheme

class SetupActivity : ComponentActivity() {

    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            RattTheme {
                var picking by remember { mutableStateOf(false) }
                if (picking) {
                    BackHandler { picking = false }
                    PickerScreen(onDone = { picking = false })
                } else {
                    SetupScreen(resumeTick.intValue, onPick = { picking = true })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}

@Composable
private fun SetupScreen(resumeTick: Int, onPick: () -> Unit) {
    val context = LocalContext.current

    // resumeTick is read so the checks re-run when the user returns from settings.
    val serviceOn = remember(resumeTick) { isServiceEnabled(context) }
    val watched = remember(resumeTick) { Watchlist.get(context) }

    var watchedLabels by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(resumeTick) {
        watchedLabels = Apps.load(context)
            .filter { it.packageName in watched }
            .map { it.label }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp),
    ) {
        Spacer(Modifier.height(28.dp))

        Text(
            text = "DOPAMINE",
            color = Bone,
            fontFamily = Display,
            fontSize = 62.sp,
            lineHeight = 58.sp,
            letterSpacing = 3.sp,
        )
        Text(
            text = "RATT!!",
            color = Ember,
            fontFamily = Display,
            fontSize = 62.sp,
            lineHeight = 58.sp,
            letterSpacing = 3.sp,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Open a watched app and this gets there first.",
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(38.dp))

        StatusRow(
            ok = serviceOn,
            label = if (serviceOn) "SERVICE RUNNING" else "SERVICE OFF",
        )
        Spacer(Modifier.height(12.dp))
        StatusRow(
            ok = watched.isNotEmpty(),
            label = when (watched.size) {
                0 -> "NO APPS SELECTED"
                1 -> "1 APP WATCHED"
                else -> "${watched.size} APPS WATCHED"
            },
        )

        if (watchedLabels.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = watchedLabels.joinToString("   ·   "),
                color = Muted,
                fontFamily = Mono,
                fontSize = 11.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(start = 21.dp),
            )
        }

        Spacer(Modifier.height(34.dp))

        if (!serviceOn) {
            Text(
                text = "Turn on the accessibility service so the app can tell which app just came to the front. It reads the package name only, never what is on your screen.",
                color = Faint,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
        }

        Action(
            label = if (serviceOn) "ACCESSIBILITY SETTINGS" else "TURN IT ON",
            emphasis = !serviceOn,
        ) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Action(label = "CHOOSE APPS", emphasis = serviceOn && watched.isEmpty(), onClick = onPick)

        Spacer(Modifier.height(10.dp))

        Action(label = "PREVIEW", emphasis = false) {
            context.startActivity(
                Intent(context, InterstitialActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PickerScreen(onDone: () -> Unit) {
    val context = LocalContext.current

    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    var watched by remember { mutableStateOf(Watchlist.get(context)) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val loaded = Apps.load(context)
        Watchlist.seedIfEmpty(context, loaded.mapTo(HashSet()) { it.packageName })
        watched = Watchlist.get(context)
        apps = loaded
    }

    val visible = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    // Watched apps float to the top so the current selection is always visible
    // without scrolling for it.
    val selected = visible.filter { it.packageName in watched }
    val rest = visible.filter { it.packageName !in watched }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WATCHED APPS",
                color = Bone,
                fontFamily = Mono,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "DONE",
                color = Ember,
                fontFamily = Mono,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDone,
                    )
                    .padding(8.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = Bone, fontSize = 18.sp),
            cursorBrush = SolidColor(Ember),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (query.isEmpty()) {
                        Text(text = "search", color = Faint, fontSize = 18.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Bone.copy(alpha = 0.14f)))
        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (selected.isNotEmpty()) {
                item(key = "head-watched") {
                    SectionHead(
                        label = "WATCHED · ${selected.size}",
                        action = "CLEAR ALL",
                        onAction = {
                            Watchlist.set(context, emptySet())
                            watched = emptySet()
                        },
                    )
                }
                items(items = selected, key = { "on-" + it.packageName }) { entry ->
                    PickerRow(entry = entry, on = true) {
                        watched = Watchlist.toggle(context, entry.packageName)
                    }
                }
            }

            if (rest.isNotEmpty()) {
                item(key = "head-all") {
                    SectionHead(label = "ALL APPS", action = null, onAction = {})
                }
                items(items = rest, key = { "off-" + it.packageName }) { entry ->
                    PickerRow(entry = entry, on = false) {
                        watched = Watchlist.toggle(context, entry.packageName)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHead(label: String, action: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Faint,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 2.5.sp,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                text = action,
                color = Ember,
                fontFamily = Mono,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun PickerRow(entry: AppEntry, on: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(vertical = 11.dp),
    ) {
        Image(
            bitmap = entry.icon,
            contentDescription = null,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = entry.label,
            color = if (on) Bone else Muted,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(
                    width = 1.dp,
                    color = if (on) Ember else Bone.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (on) {
                Box(Modifier.size(8.dp).background(Ember, CircleShape))
            }
        }
    }
}

@Composable
private fun Action(label: String, emphasis: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (emphasis) Ember else Bone.copy(alpha = 0.28f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (emphasis) Ember else Bone,
            fontFamily = Mono,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
        )
    }
}

@Composable
private fun StatusRow(ok: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .size(7.dp)
                .background(if (ok) Ember else Faint.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = if (ok) Bone else Faint,
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        )
    }
}

private fun isServiceEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val component = ComponentName(context, RattAccessibilityService::class.java)
    val full = component.flattenToString()
    val short = component.flattenToShortString()

    return enabled.split(':').any { it.equals(full, true) || it.equals(short, true) }
}
