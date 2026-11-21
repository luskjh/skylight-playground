package local.skylight.gestureswitcher.core

enum class Edge { LEFT, RIGHT }
enum class GestureAction { PREVIOUS, NEXT, OPEN_SETTINGS }

data class PointerSample(val x: Float, val y: Float)

class GestureRecognizer(
    private val edge: Edge,
    private val config: GestureConfig,
    private val density: Float,
) {
    private var startedAt = 0L
    private var starts = emptyList<PointerSample>()
    private var maxPointers = 0
    private var moved = false

    fun down(timeMs: Long, pointers: List<PointerSample>) {
        if (pointers.size > starts.size) {
            startedAt = timeMs
            starts = pointers
            moved = false
        }
        maxPointers = maxOf(maxPointers, pointers.size)
    }

    fun move(pointers: List<PointerSample>) {
        if (starts.isEmpty() || pointers.size != starts.size) return
        moved = moved || starts.indices.any { i ->
            kotlin.math.abs(pointers[i].x - starts[i].x) > dp(12) ||
                kotlin.math.abs(pointers[i].y - starts[i].y) > dp(12)
        }
    }

    fun finish(timeMs: Long, pointers: List<PointerSample>): GestureAction? {
        val requiredNavigation = config.navigationFingers.count
        val requiredSettings = when (config.settingsGesture) {
            SettingsGesture.ONE_FINGER_LONG_PRESS -> 1
            SettingsGesture.TWO_FINGER_LONG_PRESS -> 2
            SettingsGesture.THREE_FINGER_LONG_PRESS -> 3
        }
        val elapsed = timeMs - startedAt
        val result = when {
            !moved && maxPointers == requiredSettings && elapsed >= config.longPressMs ->
                GestureAction.OPEN_SETTINGS
            maxPointers == requiredNavigation && starts.size == pointers.size && isInwardSwipe(pointers) ->
                if (edge == Edge.LEFT) GestureAction.PREVIOUS else GestureAction.NEXT
            else -> null
        }
        reset()
        return result
    }

    private fun isInwardSwipe(ends: List<PointerSample>): Boolean {
        val minDistance = dp(config.sensitivity.distanceDp)
        val maxOffAxis = dp(config.sensitivity.maxOffAxisDp)
        val startX = starts.map { it.x }.average().toFloat()
        val startY = starts.map { it.y }.average().toFloat()
        val endX = ends.map { it.x }.average().toFloat()
        val endY = ends.map { it.y }.average().toFloat()
        val dx = endX - startX
        val dy = kotlin.math.abs(endY - startY)
        val inward = if (edge == Edge.LEFT) dx >= minDistance else dx <= -minDistance
        return inward && dy <= maxOffAxis
    }

    private fun dp(value: Int) = value * density
    private fun reset() { startedAt = 0; starts = emptyList(); maxPointers = 0; moved = false }
}
