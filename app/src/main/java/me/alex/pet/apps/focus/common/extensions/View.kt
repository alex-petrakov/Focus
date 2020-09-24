package me.alex.pet.apps.focus.common.extensions

import android.view.View


fun View.animateVisibility(visible: Boolean, durationMs: Long = 200) {
    val animator = this.animate().apply {
        cancel()
    }

    val targetAlpha = if (visible) 1f else 0f
    if (this.alpha == targetAlpha) return

    this.visibility = View.VISIBLE
    animator.setDuration(durationMs)
            .alpha(targetAlpha)
    if (!visible) {
        animator.withEndAction {
            this.visibility = View.GONE
        }
    }
}