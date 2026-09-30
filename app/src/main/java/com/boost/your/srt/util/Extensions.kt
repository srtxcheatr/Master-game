package com.boost.your.srt.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.boost.your.srt.data.Setting

/** Walks the ContextWrapper chain to find the hosting Activity. */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.openUrl(url: String): Boolean = runCatching {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.isSuccess

/** 754 -> "12m 34s", 45 -> "45s", 3700 -> "1h 1m 40s". */
fun Long.toDurationText(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return buildString {
        if (h > 0) append("${h}h ")
        if (h > 0 || m > 0) append("${m}m ")
        append("${s}s")
    }
}

/** Collects a DataStore-backed [Setting] as Compose state. */
@Composable
fun <T> Setting<T>.collectAsStateValue(initial: T): State<T> {
    val ctx = LocalContext.current
    val flow = remember(ctx) { flow(ctx) }
    return flow.collectAsState(initial = initial)
}
