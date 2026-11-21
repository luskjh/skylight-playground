package local.skylight.gestureswitcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import local.skylight.gestureswitcher.core.Edge

/** Draws a flush semicircular edge indicator and exposes matching hit geometry. */
class EdgeHandleView(context: Context, private val edge: Edge) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(104, 104, 104)
        setShadowLayer(dp(7f), if (edge == Edge.LEFT) dp(3f) else -dp(3f), dp(2f), Color.argb(95, 0, 0, 0))
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        alpha = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = dp(30f)
        val centerX = if (edge == Edge.LEFT) 0f else width.toFloat()
        canvas.drawCircle(centerX, height / 2f, radius, paint)
    }

    fun isInsideTouchArea(x: Float, y: Float): Boolean {
        val centerX = if (edge == Edge.LEFT) 0f else width.toFloat()
        val dx = x - centerX
        val dy = y - height / 2f
        return dx * dx + dy * dy <= width.toFloat() * width.toFloat()
    }

    private fun dp(value: Float) = value * density
}
