package il.co.radioapp.repository

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import il.co.radioapp.model.ListItem
import il.co.radioapp.model.RadioData
import il.co.radioapp.model.Station
import il.co.radioapp.util.FlexibleStringAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StationRepository {

    private var radioData: RadioData? = null
    private val stationMap = mutableMapOf<String, Station>()
    private data class Category(val id: String, val name: String, val items: List<ListItem>)
    private var structuredCategories: List<Category> = emptyList()

    // Default top-25 (used by PlayerViewModel without context)
    val top25Ids = listOf(
        "glgltz","glz","100fm","reshet-bet","reshet-aleph","88fm","kol-hamusic",
        "103fm","102fm","93fm","kol-barama","jewish-radio-kol-hai","101fm",
        "reshet-moreshet","104-5-fm","radio-99fm","hatahana","hamesh-99-5",
        "glglz-med","5radio","radio-haifa-107-5fm","radio-darom-9697fm",
        "kol-hanegev","n12","kan-11"
    )

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (radioData != null) return@withContext
        val json = context.assets.open("rlive_kcm_streams.json").bufferedReader().readText()
        val gson = GsonBuilder()
            .registerTypeHierarchyAdapter(String::class.java, FlexibleStringAdapter())
            .create()
        radioData = try { gson.fromJson(json, RadioData::class.java) } catch (e: Exception) { RadioData() }
        radioData?.stations?.forEach    { stationMap[it.id] = it }
        radioData?.kcmChannels?.forEach { stationMap[it.id] = it.toStation() }
        try {
            val root = JsonParser.parseString(json).asJsonObject
            structuredCategories = parseCategoriesArray(root.getAsJsonArray("categories"))
        } catch (_: Exception) { structuredCategories = emptyList() }
    }

    // ── Parsing ───────────────────────────────────────────────────

    private fun parseItems(arr: JsonArray?): List<ListItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (el in arr) {
                val obj = el.asJsonObject
                when (obj.get("type")?.asString) {
                    "station"     -> { val id = obj.get("id")?.asString ?: continue; stationMap[id]?.let { add(ListItem.StationItem(it)) } }
                    "separator"   -> add(ListItem.SeparatorItem(obj.get("name")?.asString ?: ""))
                    "subcategory" -> {
                        val id   = obj.get("id")?.asString ?: continue
                        val name = obj.get("name")?.asString ?: ""
                        add(ListItem.SubCategoryItem(id, name, parseItems(obj.getAsJsonArray("items"))))
                    }
                }
            }
        }
    }

    private fun parseCategoriesArray(arr: JsonArray?): List<Category> =
        arr?.map { el -> el.asJsonObject.let { obj ->
            Category(obj.get("id")?.asString ?: "", obj.get("name")?.asString ?: "", parseItems(obj.getAsJsonArray("items")))
        }} ?: emptyList()

    // ── Public API ────────────────────────────────────────────────

    fun getCategoryNames(): List<String> =
        structuredCategories.map { it.name }.ifEmpty { radioData?.categories?.keys?.toList() ?: emptyList() }

    fun getCategoryItems(name: String): List<ListItem>? =
        structuredCategories.find { it.name == name }?.items

    fun getStationsByCategory(name: String): List<Station> =
        structuredCategories.find { it.name == name }?.let { flattenStations(it.items) }
            ?: radioData?.categories?.get(name)?.mapNotNull { stationMap[it.id] } ?: emptyList()

    fun getStation(id: String): Station? = stationMap[id]

    fun getAllStations(): List<Station> = stationMap.values.toList()

    /** Non-suspend: uses hardcoded top25Ids — safe to call from ViewModel without coroutine. */
    fun getTop25(): List<Station> = top25Ids.mapNotNull { stationMap[it] }

    /** Suspend: honours user-customised top-25 saved in SharedPreferences. */
    suspend fun getTop25(context: Context): List<Station> {
        init(context)
        val saved = il.co.radioapp.AppPreferences.getTop25Ids(context)
        val ids   = if (saved.isEmpty()) top25Ids else saved
        return ids.mapNotNull { stationMap[it] }
    }

    private fun flattenStations(items: List<ListItem>): List<Station> = buildList {
        for (item in items) when (item) {
            is ListItem.StationItem     -> add(item.station)
            is ListItem.SubCategoryItem -> addAll(flattenStations(item.items))
            else -> Unit
        }
    }
}
