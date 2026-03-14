package il.co.radioapp.repository

import android.content.Context
import android.content.SharedPreferences

object FavoritesRepository {

    private const val PREFS_NAME = "radio_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_LAST_STATION = "last_station"
    private const val KEY_VOLUME = "volume"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavoriteIds(ctx: Context): MutableSet<String> =
        prefs(ctx).getStringSet(KEY_FAVORITES, mutableSetOf())!!.toMutableSet()

    fun isFavorite(ctx: Context, stationId: String) =
        getFavoriteIds(ctx).contains(stationId)

    fun toggleFavorite(ctx: Context, stationId: String): Boolean {
        val favs = getFavoriteIds(ctx)
        val added = if (favs.contains(stationId)) { favs.remove(stationId); false }
                    else { favs.add(stationId); true }
        prefs(ctx).edit().putStringSet(KEY_FAVORITES, favs).apply()
        return added
    }

    fun saveLastStation(ctx: Context, stationId: String) =
        prefs(ctx).edit().putString(KEY_LAST_STATION, stationId).apply()

    fun getLastStationId(ctx: Context): String? =
        prefs(ctx).getString(KEY_LAST_STATION, null)

    fun saveVolume(ctx: Context, vol: Float) =
        prefs(ctx).edit().putFloat(KEY_VOLUME, vol).apply()

    fun getVolume(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_VOLUME, 1.0f)
}



