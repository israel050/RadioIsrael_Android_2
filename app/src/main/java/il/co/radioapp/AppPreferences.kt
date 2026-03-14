package il.co.radioapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AppPreferences {
    private const val PREFS = "radio_israel_prefs"
    private const val KEY_THEME  = "theme"
    private const val KEY_TOP25  = "top25_ids"

    const val THEME_SYSTEM = "system"
    const val THEME_DARK   = "dark"
    const val THEME_LIGHT  = "light"

    fun getTheme(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(ctx: Context, theme: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme).apply()

    fun applyTheme(theme: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                THEME_DARK  -> AppCompatDelegate.MODE_NIGHT_YES
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else        -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun getTop25Ids(ctx: Context): List<String> {
        val json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOP25, null) ?: return emptyList()
        return Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    fun setTop25Ids(ctx: Context, ids: List<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOP25, Gson().toJson(ids)).apply()
    }
}



