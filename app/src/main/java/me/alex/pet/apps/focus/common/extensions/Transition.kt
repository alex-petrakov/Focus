package me.alex.pet.apps.focus.common.extensions

import android.view.View
import androidx.transition.Transition

fun Transition.excludeTargets(vararg targets: View, exclude: Boolean = true): Transition {
    targets.forEach { view ->
        excludeTarget(view, exclude)
    }
    return this
}

fun Transition.excludeTargets(vararg targetIds: Int, exclude: Boolean = true): Transition {
    targetIds.forEach { id ->
        excludeTarget(id, exclude)
    }
    return this
}