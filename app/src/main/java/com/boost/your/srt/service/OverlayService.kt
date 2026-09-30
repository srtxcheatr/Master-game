package com.boost.your.srt.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.boost.your.srt.MainActivity
import com.boost.your.srt.R
import com.boost.your.srt.data.AppDataStore
import com.boost.your.srt.macro.MacroButtonConfig
import com.boost.your.srt.macro.MacroEngine
import com.boost.your.srt.overlay.ControlPanelView
import com.boost.your.srt.overlay.CrosshairOverlay
import com.boost.your.srt.overlay.FpsMonitor
import com.boost.your.srt.overlay.MacroButtonOverlay
import com.boost.your.srt.overlay.MiniBubbleView
import com.boost.your.srt.overlay.OverlayLifecycleOwner
import com.boost.your.srt.shizuku.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground service that owns every overlay window:
 *  - the FPS bubble, the control panel,
 *  - the E and G macro buttons (each with its own WindowManager.LayoutParams),
 *  - the pairing crosshair.
 * Macro button state is driven by DataStore, so edits made in the settings UI move / resize the
 * live overlay immediately.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: MiniBubbleView? = null
    private lateinit var fpsMonitor: FpsMonitor
    private lateinit var macroEngine: MacroEngine
    private lateinit var lifecycleOwner: OverlayLifecycleOwner

    private var controlPanel: ControlPanelView? = null
    private var crosshair: CrosshairOverlay? = null
    private var optionsView: View? = null
    private var eBtnOverlay: MacroButtonOverlay? = null
    private var gBtnOverlay: MacroButtonOverlay? = null

    private val fpsFlow = MutableStateFlow(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val macroJobs = mutableMapOf<String, Job>()

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycleOwner().also {
            it.create()
            it.resume()
        }
        macroEngine = MacroEngine(scope)

        setupBubble()
        fpsMonitor = FpsMonitor().also {
            it.onUpdate = { fps ->
                fpsFlow.value = fps
                bubbleView?.updateFps(fps)
            }
            it.start()
        }
        observeMacroButton("E")
        observeMacroButton("G")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // onCreate may have bailed out (no overlay permission) before setting these up.
        if (!::windowManager.isInitialized || !::macroEngine.isInitialized) return START_NOT_STICKY
        if (intent?.action == ACTION_PAIR) {
            intent.getStringExtra(EXTRA_BUTTON_ID)?.let { startPairing(it) }
        }
        return START_STICKY
    }

    // ------------------------------------------------------------ Bubble

    private fun setupBubble() {
        scope.launch {
            val x = AppDataStore.bubbleX.get(this@OverlayService)
            val y = AppDataStore.bubbleY.get(this@OverlayService)
            val view = MiniBubbleView(this@OverlayService).apply {
                onLogoClick = { toggleControlPanel() }
                onLongPress = { showBubbleOptions() }
                onMoved = { nx, ny ->
                    scope.launch {
                        AppDataStore.bubbleX.set(this@OverlayService, nx)
                        AppDataStore.bubbleY.set(this@OverlayService, ny)
                    }
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
            }
            runCatching { windowManager.addView(view, params) }.onSuccess {
                bubbleView = view
                view.updateFps(fpsFlow.value)
            }
        }
    }

    /** Long press on the bubble: small "Stop Service" chip next to it, auto-dismissed. */
    private fun showBubbleOptions() {
        dismissOptions()
        val bubble = bubbleView ?: return
        val bp = bubble.layoutParams as? WindowManager.LayoutParams ?: return
        val d = resources.displayMetrics.density
        val chip = TextView(this).apply {
            text = "⏻  Stop Service"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 12 * d
                setColor(Color.parseColor("#F0FF3B5C"))
            }
            setOnClickListener {
                dismissOptions()
                stopSelf()
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bp.x
            y = bp.y + (52 * d).toInt()
        }
        runCatching { windowManager.addView(chip, params) }.onSuccess {
            optionsView = chip
            mainHandler.postDelayed({ dismissOptions() }, 4000)
        }
    }

    private fun dismissOptions() {
        optionsView?.let { v -> runCatching { windowManager.removeView(v) } }
        optionsView = null
    }

    // ------------------------------------------------------------ Control panel

    private fun toggleControlPanel() {
        if (controlPanel != null) closeControlPanel() else openControlPanel()
    }

    private fun openControlPanel() {
        dismissOptions()
        val dm = resources.displayMetrics
        val panel = ControlPanelView(
            context = this,
            macroEngine = macroEngine,
            owner = lifecycleOwner,
            fps = fpsFlow.asStateFlow(),
            onPairRequest = { id ->
                closeControlPanel()
                startPairing(id)
            },
            onScreenshot = { takeScreenshot() }
        ).apply { onClose = { closeControlPanel() } }

        val params = WindowManager.LayoutParams(
            (dm.widthPixels * 0.85f).toInt(),
            (dm.heightPixels * 0.75f).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }

        runCatching { windowManager.addView(panel, params) }.onSuccess {
            controlPanel = panel
            panel.slideUp()
        }.onFailure {
            Toast.makeText(this, "Could not open panel: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeControlPanel() {
        val panel = controlPanel ?: return
        controlPanel = null
        panel.animate().translationY(panel.height.toFloat()).setDuration(200).withEndAction {
            runCatching { windowManager.removeView(panel) }
        }.start()
    }

    // ------------------------------------------------------------ Macro buttons

    /** Keeps the overlay for one button in sync with its DataStore config. E and G never share state. */
    private fun observeMacroButton(id: String) {
        macroJobs[id]?.cancel()
        macroJobs[id] = scope.launch {
            AppDataStore.macroConfigFlow(this@OverlayService, id).collect { cfg ->
                val existing = if (id == "E") eBtnOverlay else gBtnOverlay
                when {
                    cfg.isEnabled && existing == null -> addMacroButton(cfg)
                    cfg.isEnabled && existing != null -> existing.applyConfig(windowManager, cfg)
                    !cfg.isEnabled && existing != null -> removeMacroButton(id)
                }
            }
        }
    }

    private fun addMacroButton(cfg: MacroButtonConfig) {
        val view = MacroButtonOverlay(
            context = this,
            initial = cfg,
            engine = macroEngine,
            onPositionCommitted = { id, x, y ->
                scope.launch { AppDataStore.updateMacroConfig(this@OverlayService, id) { it.copy(posX = x, posY = y) } }
            },
            onLockToggled = { id, locked ->
                scope.launch { AppDataStore.updateMacroConfig(this@OverlayService, id) { it.copy(isLocked = locked) } }
            }
        )
        runCatching { windowManager.addView(view, view.params) }.onSuccess {
            if (cfg.id == "E") eBtnOverlay = view else gBtnOverlay = view
        }
    }

    private fun removeMacroButton(id: String) {
        macroEngine.stop(id)
        val view = if (id == "E") eBtnOverlay else gBtnOverlay
        if (id == "E") eBtnOverlay = null else gBtnOverlay = null
        view?.let { runCatching { windowManager.removeView(it) } }
    }

    // ------------------------------------------------------------ Pairing

    private fun startPairing(id: String) {
        dismissCrosshair()
        scope.launch {
            val cfg = AppDataStore.getMacroConfig(this@OverlayService, id)
            val dm = resources.displayMetrics
            val startX = if (cfg.isPaired) cfg.targetX else dm.widthPixels / 2
            val startY = if (cfg.isPaired) cfg.targetY else dm.heightPixels / 2
            val view = CrosshairOverlay(
                context = this@OverlayService,
                buttonId = id,
                initialX = startX,
                initialY = startY,
                onConfirm = { x, y ->
                    scope.launch {
                        AppDataStore.updateMacroConfig(this@OverlayService, id) {
                            it.copy(targetX = x, targetY = y, isPaired = true)
                        }
                        Toast.makeText(this@OverlayService, "Button $id paired: X:$x Y:$y", Toast.LENGTH_SHORT).show()
                    }
                    dismissCrosshair()
                },
                onCancel = { dismissCrosshair() }
            )
            runCatching { windowManager.addView(view, view.layoutParams()) }.onSuccess { crosshair = view }
        }
    }

    private fun dismissCrosshair() {
        crosshair?.let { v -> runCatching { windowManager.removeView(v) } }
        crosshair = null
    }

    // ------------------------------------------------------------ Screenshot

    private fun takeScreenshot() {
        closeControlPanel()
        scope.launch {
            // Give the panel time to slide away so it is not in the picture.
            kotlinx.coroutines.delay(450)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val path = "/sdcard/Pictures/BoostMaster_$stamp.png"
            val r = withContext(Dispatchers.IO) { ShizukuHelper.exec("screencap -p $path") }
            val msg = if (r.exitCode == 0) "Screenshot saved: Pictures/BoostMaster_$stamp.png"
            else "Screenshot failed: ${r.error ?: r.output.ifBlank { "exit ${r.exitCode}" }}"
            Toast.makeText(this@OverlayService, msg, Toast.LENGTH_LONG).show()
        }
    }

    // ------------------------------------------------------------ Foreground notification

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ------------------------------------------------------------ Teardown

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (::fpsMonitor.isInitialized) fpsMonitor.stop()
        if (::macroEngine.isInitialized) macroEngine.stopAll()
        macroJobs.values.forEach { it.cancel() }
        scope.cancel()

        if (::windowManager.isInitialized) {
            listOf<View?>(bubbleView, controlPanel, eBtnOverlay, gBtnOverlay, crosshair, optionsView)
                .filterNotNull()
                .filter { it.isAttachedToWindow }
                .forEach { runCatching { windowManager.removeView(it) } }
        }
        bubbleView = null
        controlPanel = null
        eBtnOverlay = null
        gBtnOverlay = null
        crosshair = null
        optionsView = null
        if (::lifecycleOwner.isInitialized) lifecycleOwner.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.boost.your.srt.action.STOP_OVERLAY"
        const val ACTION_PAIR = "com.boost.your.srt.action.PAIR"
        const val EXTRA_BUTTON_ID = "button_id"

        /** Starts the service if needed and shows the pairing crosshair for button [id] ("E" or "G"). */
        fun pair(context: Context, id: String) {
            val i = Intent(context, OverlayService::class.java).setAction(ACTION_PAIR).putExtra(EXTRA_BUTTON_ID, id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
        }

        fun start(context: Context) {
            val i = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
