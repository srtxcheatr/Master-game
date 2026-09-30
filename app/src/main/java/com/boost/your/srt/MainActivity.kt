package com.boost.your.srt

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.boost.your.srt.display.StageStretchScreen
import com.boost.your.srt.macro.MacroSettingsScreen
import com.boost.your.srt.service.OverlayService
import com.boost.your.srt.shizuku.ShizukuHelper
import com.boost.your.srt.shizuku.ShizukuSetupScreen
import com.boost.your.srt.ui.screens.HomeScreen
import com.boost.your.srt.ui.theme.BgPrimary
import com.boost.your.srt.ui.theme.BoostMasterTheme
import com.boost.your.srt.util.PermissionHelper

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STAGE = "stage"
    const val MACRO = "macro"
    const val SHIZUKU = "shizuku"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoostMasterTheme {
                Box(Modifier.fillMaxSize().background(BgPrimary)) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val nav = rememberNavController()

    // Android 13+: notification permission for the foreground-service notification
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PermissionHelper.hasPostNotifications(ctx)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Overlay permission: start the service if granted, otherwise open the system screen once.
    var askedOverlay by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (PermissionHelper.canDrawOverlays(ctx)) {
            OverlayService.start(ctx)
        } else if (!askedOverlay) {
            askedOverlay = true
            ctx.startActivity(PermissionHelper.overlaySettingsIntent(ctx))
        }
    }

    // Shizuku: if it is not running at startup, show the setup guide (once per launch).
    var checkedShizuku by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!checkedShizuku) {
            checkedShizuku = true
            if (!ShizukuHelper.isAvailable()) nav.navigate(Routes.SHIZUKU)
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                initialSection = 0,
                onOpenMacro = { nav.navigate(Routes.MACRO) },
                onOpenShizuku = { nav.navigate(Routes.SHIZUKU) }
            )
        }
        composable(Routes.SETTINGS) {
            // Home with the Quick Access Tools section selected
            HomeScreen(
                initialSection = 1,
                onOpenMacro = { nav.navigate(Routes.MACRO) },
                onOpenShizuku = { nav.navigate(Routes.SHIZUKU) }
            )
        }
        composable(Routes.STAGE) { StageStretchScreen() }
        composable(Routes.MACRO) {
            MacroSettingsScreen(onPair = { id -> OverlayService.pair(ctx, id) })
        }
        composable(Routes.SHIZUKU) {
            ShizukuSetupScreen(
                onContinue = { leaveShizuku(nav) },
                onSkip = { leaveShizuku(nav) }
            )
        }
    }
}

private fun leaveShizuku(nav: NavHostController) {
    if (!nav.popBackStack()) nav.navigate(Routes.HOME)
}
