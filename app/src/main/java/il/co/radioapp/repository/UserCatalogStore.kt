package il.co.radioapp.repository

import android.content.Context
import com.google.gson.Gson

data class UserStation(
    val id: String                          = "",
    val nameOverride: String?               = null,
    val streamUrlOverride: String?          = null,
    val logoUrlOverride: String?            = null,
    val isHidden: Boolean                   = false,
    val isCustom: Boolean                   = false
)

data class UserNode(
    val id: String,
    val type: String,
    val nameOverride: String?  = null,
    val orderIndex: Int        = 0,
    val parentId: String?      = null,
    val isDeleted: Boolean     = false,
    val isCustom: Boolean      = false
)

/** override / add / delete לפריט בתוך קטגוריה */
data class CategoryItemOverride(
    val categoryId: String,
    val itemId: String,
    val itemType: String,                   // "station"|"subcategory"|"separator"
    val nameOverride: String?  = null,
    val orderIndex: Int        = 0,
    val isHidden: Boolean      = false,
    val isDeleted: Boolean     = false,
    val isCustom: Boolean      = false
)

data class UserCatalog(
    val version: Int                                    = 1,
    val stationOverrides: Map<String, UserStation>      = emptyMap(),
    val nodeOverrides: List<UserNode>                   = emptyList(),
    val categoryItemOverrides: List<CategoryItemOverride> = emptyList()
)

object UserCatalogStore {
    private const val PREFS = "radio_israel_prefs"
    private const val KEY   = "user_catalog_v1"
    private val gson = Gson()

    fun load(context: Context): UserCatalog {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return UserCatalog()
        return try { gson.fromJson(json, UserCatalog::class.java) }
        catch (_: Exception) { UserCatalog() }
    }

    fun save(context: Context, catalog: UserCatalog) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, gson.toJson(catalog)).apply()
    }

    fun reset(context: Context) = save(context, UserCatalog())
}
