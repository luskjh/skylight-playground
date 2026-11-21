package local.skylight.gestureswitcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Transparent trampoline used because task reordering can ignore service-supplied animations. */
class TransitionActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return finish()
        val activityName = intent.getStringExtra(EXTRA_ACTIVITY) ?: return finish()
        val direction = intent.getIntExtra(EXTRA_DIRECTION, 1)

        val target = Intent(Intent.ACTION_MAIN)
            .setClassName(packageName, activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(target)
        if (direction > 0) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        window.decorView.postDelayed({
            finish()
            overridePendingTransition(0, 0)
        }, 320)
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_ACTIVITY = "activity"
        const val EXTRA_DIRECTION = "direction"
    }
}
