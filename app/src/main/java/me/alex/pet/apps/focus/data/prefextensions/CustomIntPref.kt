package me.alex.pet.apps.focus.data.prefextensions

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.execute
import com.chibatching.kotpref.pref.AbstractPref
import kotlin.reflect.KProperty

class CustomIntPref<T>(
        val default: T,
        override val key: String?,
        private val adapter: Adapter<T, Int>,
        private val commitByDefault: Boolean
) : AbstractPref<T>() {

    private val defaultInt by lazy { adapter.toPref(default) }

    override fun getFromPreference(property: KProperty<*>, preference: SharedPreferences): T {
        return adapter.fromPref(preference.getInt(preferenceKey, defaultInt))
    }

    override fun setToEditor(property: KProperty<*>, value: T, editor: SharedPreferences.Editor) {
        editor.putInt(key, adapter.toPref(value))
    }

    @SuppressLint("CommitPrefEdits")
    override fun setToPreference(property: KProperty<*>, value: T, preference: SharedPreferences) {
        preference.edit().putInt(preferenceKey, adapter.toPref(value)).execute(commitByDefault)
    }
}

fun <T> KotprefModel.intPref(
        default: T,
        key: String,
        adapter: Adapter<T, Int>,
        commitByDefault: Boolean = commitAllPropertiesByDefault
): CustomIntPref<T> {
    return CustomIntPref(default, key, adapter, commitByDefault)
}

fun <T> KotprefModel.intPref(
        default: T,
        key: Int,
        adapter: Adapter<T, Int>,
        commitByDefault: Boolean = false
): CustomIntPref<T> {
    return CustomIntPref(default, context.getString(key), adapter, commitByDefault)
}