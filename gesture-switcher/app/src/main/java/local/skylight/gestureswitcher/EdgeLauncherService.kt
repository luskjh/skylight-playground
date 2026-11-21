package local.skylight.gestureswitcher

import android.app.*
import android.content.ComponentName
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import local.skylight.gestureswitcher.core.*

class EdgeLauncherService : Service() {
    private lateinit var windows: WindowManager
    private val overlays = mutableListOf<View>()
    private lateinit var config: LauncherConfig
    private lateinit var carousel: Carousel<AppSlot>

    override fun onCreate() {
        super.onCreate()
        config = ConfigStore(this).load()
        if (!config.isReady || !android.provider.Settings.canDrawOverlays(this)) {
            openSettings(); stopSelf(); return
        }
        carousel = Carousel(config.slots, config.defaultIndex)
        startForeground(NOTIFICATION_ID, notification())
        windows = getSystemService(WINDOW_SERVICE) as WindowManager
        addEdge(Edge.LEFT)
        addEdge(Edge.RIGHT)
    }

    private fun addEdge(edge: Edge) {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val root = EdgeHandleView(this, edge)
        val recognizer = GestureRecognizer(edge, config.gestures, density)
        val fade = Runnable { root.animate().alpha(0f).setDuration(600).start() }
        root.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && !root.isInsideTouchArea(event.x, event.y)) {
                return@setOnTouchListener false
            }
            val points = event.points()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    root.removeCallbacks(fade); root.animate().cancel()
                    root.animate().alpha(0.82f).setDuration(120).start()
                    recognizer.down(event.eventTime, points)
                }
                MotionEvent.ACTION_MOVE -> recognizer.move(points)
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                    root.postDelayed(fade, 900)
                    when (val action = recognizer.finish(event.eventTime, points)) {
                        GestureAction.NEXT -> { Log.i(TAG, "Recognized NEXT from $edge"); navigate(1) }
                        GestureAction.PREVIOUS -> { Log.i(TAG, "Recognized PREVIOUS from $edge"); navigate(-1) }
                        GestureAction.OPEN_SETTINGS -> { Log.i(TAG, "Recognized OPEN_SETTINGS from $edge"); openSettings() }
                        null -> Log.d(TAG, "Gesture not recognized on $edge; pointers=${points.size}")
                    }
                }
                MotionEvent.ACTION_CANCEL -> root.postDelayed(fade, 200)
            }
            true
        }
        val touchRadius = dp(config.gestures.edgeWidth.dp)
        val params = WindowManager.LayoutParams(
            touchRadius, touchRadius * 2,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = (if (edge == Edge.LEFT) Gravity.LEFT else Gravity.RIGHT) or Gravity.CENTER_VERTICAL }
        windows.addView(root, params)
        overlays += root
    }

    private fun navigate(delta: Int) {
        repeat(carousel.size) {
            val slot = carousel.move(delta) ?: return
            val intent = launchIntent(slot)
            if (packageManager.resolveActivity(intent, 0) != null) {
                startActivity(Intent(this, TransitionActivity::class.java)
                    .putExtra(TransitionActivity.EXTRA_PACKAGE, slot.packageName)
                    .putExtra(TransitionActivity.EXTRA_ACTIVITY, slot.activityName)
                    .putExtra(TransitionActivity.EXTRA_DIRECTION, delta)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                return
            }
        }
        openSettings()
    }

    private fun launchIntent(slot: AppSlot) = Intent(Intent.ACTION_MAIN)
        .setComponent(ComponentName(slot.packageName, slot.activityName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Edge Launcher", NotificationManager.IMPORTANCE_MIN)
        )
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Edge Launcher active")
            .setContentText("Two-finger edge gestures enabled")
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (::windows.isInitialized) overlays.forEach { runCatching { windows.removeView(it) } }
        overlays.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun MotionEvent.points() = (0 until pointerCount).map { PointerSample(getX(it), getY(it)) }

    companion object {
        const val CHANNEL = "edge-launcher"
        const val NOTIFICATION_ID = 1
        const val TAG = "EdgeLauncher"
    }
}
