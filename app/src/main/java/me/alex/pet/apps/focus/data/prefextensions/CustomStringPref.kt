package me.alex.pet.apps.focus.data.prefextensions

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.execute
import com.chibatching.kotpref.pref.AbstractPref
import kotlin.reflect.KProperty

class CustomStringPref<T>(
        val default: T,
        override val key: String?,
        private val adapter: Adapter<T, String>,
        private val commitByDefault: Boolean
) : AbstractPref<T>() {

    private val defaultString by lazy { adapter.toPref(default) }

    override fun getFromPreference(property: KProperty<*>, preference: SharedPreferences): T {
        val string = requireNotNull(preference.getString(preferenceKey, defaultString))
        return adapter.fromPref(string)
    }

    override fun setToEditor(property: KProperty<*>, value: T, editor: SharedPreferences.Editor) {
        editor.putString(key, adapter.toPref(value))
    }

    @SuppressLint("CommitPrefEdits")
    override fun setToPreference(property: KProperty<*>, value: T, preference: SharedPreferences) {
        preference.edit().putString(preferenceKey, adapter.toPref(value)).execute(commitByDefault)
    }
}

fun <T> KotprefModel.stringPref(
        default: T,
        key: String,
        adapter: Adapter<T, String>,
        commitByDefault: Boolean = commitAllPropertiesByDefault
): CustomStringPref<T> {
    return CustomStringPref(default, key, adapter, commitByDefault)
}

fun <T> KotprefModel.stringPref(
        default: T,
        key: Int,
        adapter: Adapter<T, String>,
        commitByDefault: Boolean = false
): CustomStringPref<T> {
    return CustomStringPref(default, context.getString(key), adapter, commitByDefault)
}