package local.skylight.gestureswitcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureRecognizerTest {
    private val config = GestureConfig()

    @Test fun twoFingerLeftEdgeSwipeMeansPrevious() {
        val r = GestureRecognizer(Edge.LEFT, config, 1f)
        r.down(0, points(2, 4f))
        assertEquals(GestureAction.PREVIOUS, r.finish(300, points(2, 120f)))
    }

    @Test fun twoFingerRightEdgeSwipeMeansNext() {
        val r = GestureRecognizer(Edge.RIGHT, config, 1f)
        r.down(0, points(2, 200f))
        assertEquals(GestureAction.NEXT, r.finish(300, points(2, 80f)))
    }

    @Test fun oneFingerSwipeIsIgnored() {
        val r = GestureRecognizer(Edge.LEFT, config, 1f)
        r.down(0, points(1, 4f))
        assertNull(r.finish(300, points(1, 120f)))
    }

    @Test fun oneFingerLongPressOpensSettings() {
        val r = GestureRecognizer(Edge.LEFT, config, 1f)
        r.down(0, points(1, 4f))
        assertEquals(GestureAction.OPEN_SETTINGS, r.finish(700, points(1, 4f)))
    }

    @Test fun swipeUsesCentroidInsteadOfRequiringIdenticalFingerTravel() {
        val r = GestureRecognizer(Edge.LEFT, config, 1f)
        r.down(0, listOf(PointerSample(4f, 100f), PointerSample(4f, 140f)))
        assertEquals(GestureAction.PREVIOUS, r.finish(300,
            listOf(PointerSample(124f, 100f), PointerSample(84f, 140f))))
    }

    @Test fun verticalMovementDoesNotNavigate() {
        val r = GestureRecognizer(Edge.LEFT, config, 1f)
        r.down(0, listOf(PointerSample(4f, 10f), PointerSample(4f, 40f)))
        assertNull(r.finish(300, listOf(PointerSample(120f, 250f), PointerSample(120f, 280f))))
    }

    private fun points(count: Int, x: Float) =
        (0 until count).map { PointerSample(x, 100f + it * 30f) }
}
