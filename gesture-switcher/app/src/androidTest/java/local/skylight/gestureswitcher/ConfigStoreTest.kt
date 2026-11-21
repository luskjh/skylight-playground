package local.skylight.gestureswitcher

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import local.skylight.gestureswitcher.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigStoreTest {
    @Test fun roundTripsOrderedSlotsDefaultAndGesturePresets() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val expected = LauncherConfig(
            slots = listOf(
                AppSlot("One", "test.one", "test.one.Main"),
                AppSlot("Two", "test.two", "test.two.Main"),
                AppSlot("Three", "test.three", "test.three.Main"),
            ),
            defaultIndex = 1,
            gestures = GestureConfig(
                navigationFingers = FingerCount.THREE,
                sensitivity = SwipeSensitivity.DELIBERATE,
                edgeWidth = EdgeWidth.WIDE,
                settingsGesture = SettingsGesture.THREE_FINGER_LONG_PRESS,
            )
        )
        val store = ConfigStore(context)
        store.save(expected)
        val actual = store.load()
        assertEquals(expected, actual)
        assertTrue(actual.isReady)
    }
}
