package me.alex.pet.apps.focus.common.extensions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observe(viewLifecycleOwner: LifecycleOwner, onChanged: (T) -> Unit) {
    this.observe(viewLifecycleOwner, Observer { t -> onChanged(t) })
}