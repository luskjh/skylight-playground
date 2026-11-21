package local.skylight.gestureswitcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarouselTest {
    @Test fun wrapsForwardAndBackward() {
        val carousel = Carousel(listOf("A", "B", "C"))
        assertEquals("B", carousel.next())
        assertEquals("C", carousel.next())
        assertEquals("A", carousel.next())
        assertEquals("C", carousel.previous())
    }

    @Test fun supportsArbitrarySlotCountAndStartIndex() {
        val carousel = Carousel((0 until 12).toList(), 10)
        assertEquals(10, carousel.current)
        assertEquals(11, carousel.next())
        assertEquals(0, carousel.next())
    }

    @Test fun emptyCarouselIsSafe() {
        val carousel = Carousel<String>(emptyList())
        assertNull(carousel.next())
        assertNull(carousel.previous())
    }
}
