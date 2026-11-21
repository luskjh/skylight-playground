package local.skylight.gestureswitcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import local.skylight.gestureswitcher.core.*

class MainActivity : Activity() {
    private val store by lazy { ConfigStore(this) }
    private val slots = mutableListOf<AppSlot>()
    private var defaultIndex = 0
    private lateinit var slotContainer: LinearLayout
    private lateinit var addButton: Button
    private lateinit var status: TextView
    private lateinit var fingers: Spinner
    private lateinit var sensitivity: Spinner
    private lateinit var edgeWidth: Spinner
    private lateinit var settingsGesture: Spinner

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val existing = store.load()
        slots += existing.slots.take(MAX_SLOTS)
        defaultIndex = existing.defaultIndex.coerceIn(0, maxOf(0, slots.lastIndex))
        setContentView(buildUi(existing.gestures))
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) updateStatus()
    }

    private fun buildUi(gestures: GestureConfig): View {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(24), dp(32), dp(32))
        }
        root.addView(TextView(this).apply { text = "Edge Launcher"; textSize = 32f })
        root.addView(TextView(this).apply {
            text = "Choose up to three apps. Right-edge swipe moves forward; left-edge swipe moves back."
            textSize = 17f; setPadding(0, dp(8), 0, dp(20))
        })
        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, dp(16)) }
        root.addView(status)
        root.addView(TextView(this).apply { text = "Workspaces"; textSize = 22f })
        slotContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(slotContainer)
        addButton = Button(this).apply {
            text = "Add workspace"
            setOnClickListener { if (slots.size < MAX_SLOTS) chooseApp(null) }
        }
        root.addView(addButton)

        root.addView(TextView(this).apply { text = "Gesture presets"; textSize = 22f; setPadding(0, dp(20), 0, 0) })
        fingers = enumSpinner("Navigation fingers", FingerCount.entries.toTypedArray(), gestures.navigationFingers, root)
        sensitivity = enumSpinner("Swipe sensitivity", SwipeSensitivity.entries.toTypedArray(), gestures.sensitivity, root)
        edgeWidth = enumSpinner("Edge touch zone", EdgeWidth.entries.toTypedArray(), gestures.edgeWidth, root)
        settingsGesture = enumSpinner("Open settings", SettingsGesture.entries.toTypedArray(), gestures.settingsGesture, root)

        root.addView(Button(this).apply {
            text = "Save, apply, and launch"
            setOnClickListener { saveAndStart() }
        })
        renderSlots()
        updateStatus()
        return ScrollView(this).apply { addView(root) }
    }

    private fun renderSlots() {
        slotContainer.removeAllViews()
        if (::addButton.isInitialized) addButton.isEnabled = slots.size < MAX_SLOTS
        slots.forEachIndexed { index, slot ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(RadioButton(this).apply {
                isChecked = index == defaultIndex
                contentDescription = "Default workspace"
                setOnClickListener { defaultIndex = index; renderSlots() }
            })
            row.addView(Button(this).apply {
                text = "${index + 1}. ${slot.label}"
                setOnClickListener { chooseApp(index) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Button(this).apply { text = "↑"; isEnabled = index > 0; setOnClickListener { move(index, -1) } })
            row.addView(Button(this).apply { text = "↓"; isEnabled = index < slots.lastIndex; setOnClickListener { move(index, 1) } })
            row.addView(Button(this).apply { text = "×"; setOnClickListener { remove(index) } })
            slotContainer.addView(row)
        }
    }

    private fun move(index: Int, delta: Int) {
        val destination = index + delta
        val item = slots.removeAt(index); slots.add(destination, item)
        defaultIndex = when (defaultIndex) { index -> destination; destination -> index; else -> defaultIndex }
        renderSlots()
    }

    private fun remove(index: Int) {
        slots.removeAt(index)
        defaultIndex = defaultIndex.coerceIn(0, maxOf(0, slots.lastIndex))
        renderSlots()
    }

    private fun chooseApp(replaceIndex: Int?) {
        val apps = availableApps()
        AlertDialog.Builder(this)
            .setTitle("Choose workspace app")
            .setItems(apps.map { it.label }.toTypedArray()) { _, which ->
                if (replaceIndex == null) slots += apps[which] else slots[replaceIndex] = apps[which]
                renderSlots()
            }.show()
    }

    private fun availableApps(): List<AppSlot> {
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        )
        return intents.flatMap { packageManager.queryIntentActivities(it, 0) }
            .distinctBy { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .filter { it.activityInfo.packageName != packageName }
            .map { it.toSlot() }
            .sortedBy { it.label.lowercase() }
    }

    private fun ResolveInfo.toSlot() = AppSlot(
        loadLabel(packageManager).toString(), activityInfo.packageName, activityInfo.name
    )

    private fun saveAndStart() {
        if (slots.isEmpty()) { Toast.makeText(this, "Choose at least one workspace", Toast.LENGTH_SHORT).show(); return }
        val config = LauncherConfig(
            slots.toList(), defaultIndex,
            GestureConfig(
                fingers.selectedItem as FingerCount,
                sensitivity.selectedItem as SwipeSensitivity,
                edgeWidth.selectedItem as EdgeWidth,
                settingsGesture.selectedItem as SettingsGesture,
            )
        )
        store.save(config)
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        stopService(Intent(this, EdgeLauncherService::class.java))
        val service = Intent(this, EdgeLauncherService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
        launch(config.defaultSlot!!)
        finish()
    }

    private fun launch(slot: AppSlot) {
        startActivity(Intent(Intent.ACTION_MAIN).setComponent(ComponentName(slot.packageName, slot.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }

    private fun updateStatus() {
        status.text = when {
            !Settings.canDrawOverlays(this) -> "Setup needed: allow Display over other apps."
            slots.isEmpty() -> "Setup needed: choose workspace apps."
            else -> "Ready. Long-press with the configured finger count on either edge to return here."
        }
    }

    private fun <T : Enum<T>> enumSpinner(label: String, values: Array<T>, selected: T, parent: LinearLayout): Spinner {
        parent.addView(TextView(this).apply { text = label; textSize = 16f })
        return Spinner(this).also { spinner ->
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, values)
            spinner.setSelection(values.indexOf(selected))
            parent.addView(spinner)
        }
    }

    companion object { const val MAX_SLOTS = 3 }
}
