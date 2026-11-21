package local.skylight.gestureswitcher.core

data class AppSlot(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val key: String get() = "$packageName/$activityName"
}

enum class FingerCount(val count: Int, private val display: String) {
    TWO(2, "Two fingers"), THREE(3, "Three fingers");
    override fun toString() = display
}
enum class SwipeSensitivity(val distanceDp: Int, val maxOffAxisDp: Int, private val display: String) {
    RELAXED(32, 120, "Relaxed"), STANDARD(48, 96, "Standard"), DELIBERATE(80, 72, "Deliberate");
    override fun toString() = display
}
enum class EdgeWidth(val dp: Int, private val display: String) {
    NARROW(72, "Narrow"), STANDARD(120, "Standard"), WIDE(180, "Wide");
    override fun toString() = display
}
enum class SettingsGesture(private val display: String) {
    ONE_FINGER_LONG_PRESS("One-finger long press"),
    TWO_FINGER_LONG_PRESS("Two-finger long press"),
    THREE_FINGER_LONG_PRESS("Three-finger long press");
    override fun toString() = display
}

data class GestureConfig(
    val navigationFingers: FingerCount = FingerCount.TWO,
    val sensitivity: SwipeSensitivity = SwipeSensitivity.STANDARD,
    val edgeWidth: EdgeWidth = EdgeWidth.STANDARD,
    val settingsGesture: SettingsGesture = SettingsGesture.ONE_FINGER_LONG_PRESS,
    val longPressMs: Long = 650,
)

data class LauncherConfig(
    val slots: List<AppSlot> = emptyList(),
    val defaultIndex: Int = 0,
    val gestures: GestureConfig = GestureConfig(),
) {
    val isReady: Boolean get() = slots.isNotEmpty() && defaultIndex in slots.indices
    val defaultSlot: AppSlot? get() = slots.getOrNull(defaultIndex)
}
