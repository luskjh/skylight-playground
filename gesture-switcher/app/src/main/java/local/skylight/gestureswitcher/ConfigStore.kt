package local.skylight.gestureswitcher

import android.content.Context
import local.skylight.gestureswitcher.core.*
import org.json.JSONArray
import org.json.JSONObject

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("edge_launcher", Context.MODE_PRIVATE)

    fun load(): LauncherConfig = runCatching {
        val root = JSONObject(prefs.getString("config", "{}") ?: "{}")
        val slotsJson = root.optJSONArray("slots") ?: JSONArray()
        val slots = (0 until slotsJson.length()).map { i ->
            slotsJson.getJSONObject(i).let {
                AppSlot(it.getString("label"), it.getString("package"), it.getString("activity"))
            }
        }
        val gestures = root.optJSONObject("gestures")
        LauncherConfig(
            slots = slots,
            defaultIndex = root.optInt("defaultIndex", 0),
            gestures = GestureConfig(
                navigationFingers = enumValueOr(gestures?.optString("fingers"), FingerCount.TWO),
                sensitivity = enumValueOr(gestures?.optString("sensitivity"), SwipeSensitivity.STANDARD),
                edgeWidth = enumValueOr(gestures?.optString("edgeWidth"), EdgeWidth.STANDARD),
                settingsGesture = migratedSettingsGesture(gestures?.optString("settingsGesture")),
            )
        )
    }.getOrDefault(LauncherConfig())

    fun save(config: LauncherConfig) {
        val slots = JSONArray().apply {
            config.slots.forEach { slot ->
                put(JSONObject().apply {
                    put("label", slot.label); put("package", slot.packageName); put("activity", slot.activityName)
                })
            }
        }
        val gestures = JSONObject().apply {
            put("fingers", config.gestures.navigationFingers.name)
            put("sensitivity", config.gestures.sensitivity.name)
            put("edgeWidth", config.gestures.edgeWidth.name)
            put("settingsGesture", config.gestures.settingsGesture.name)
        }
        val root = JSONObject().apply {
            put("slots", slots); put("defaultIndex", config.defaultIndex); put("gestures", gestures)
        }
        check(prefs.edit().putString("config", root.toString()).commit()) {
            "Unable to save Edge Launcher configuration"
        }
    }

    private fun migratedSettingsGesture(value: String?): SettingsGesture = when (value) {
        // Migrate the early development default, which proved too difficult on hardware.
        null, SettingsGesture.TWO_FINGER_LONG_PRESS.name -> SettingsGesture.ONE_FINGER_LONG_PRESS
        else -> enumValueOr(value, SettingsGesture.ONE_FINGER_LONG_PRESS)
    }

    private inline fun <reified T : Enum<T>> enumValueOr(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
