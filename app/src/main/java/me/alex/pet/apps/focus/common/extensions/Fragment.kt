package me.alex.pet.apps.focus.common.extensions

import android.content.Context
import androidx.fragment.app.Fragment

fun Fragment.requireAppContext(): Context = requireContext().applicationContext