package il.co.radioapp.repository

import android.content.Context
import il.co.radioapp.model.ListItem
import il.co.radioapp.model.Station
import java.util.UUID

enum class NodeType { STATION, SUBCATEGORY, SEPARATOR }

data class ResolvedCategory(
    val id: String,
    val displayName: String,
    val orderIndex: Int   = 0,
    val isCustom: Boolean = false
)

data class ResolvedNode(
    val id: String,
    val displayName: String,
    val type: NodeType,
    val isHidden: Boolean  = false,
    val isCustom: Boolean  = false,
    val orderIndex: Int    = 0
)

object CatalogRepository {

    private var userCatalog: UserCatalog = UserCatalog()

    // ── אתחול ────────────────────────────────────────────────────────

    suspend fun init(context: Context) {
        StationRepository.init(context)
        userCatalog = UserCatalogStore.load(context)
    }

    // ── שאילתות בסיסיות ──────────────────────────────────────────────

    fun getCategoryNames(): List<String>   = resolveCategories().map { it.id }
    fun getCategoryItems(name: String)     = StationRepository.getCategoryItems(name)
    fun getStationsByCategory(name: String)= StationRepository.getStationsByCategory(name)
    fun getStation(id: String): Station?   = StationRepository.getStation(id)?.let { applyUserOverride(it) }
        ?: userCatalog.stationOverrides[id]?.let { ov ->
            if (!ov.isCustom) null
            else Station(id, ov.nameOverride ?: id, ov.streamUrlOverride ?: "", ov.logoUrlOverride)
        }
    fun getAllStations(): List<Station>     = buildList {
        addAll(StationRepository.getAllStations().filter { !isHidden(it.id) }.map { applyUserOverride(it) })
        // custom stations
        userCatalog.stationOverrides.values.filter { it.isCustom && !it.isHidden }
            .forEach { ov -> add(Station(ov.id, ov.nameOverride ?: ov.id, ov.streamUrlOverride ?: "", ov.logoUrlOverride)) }
    }
    fun getTop25(): List<Station>          = StationRepository.getTop25().map { applyUserOverride(it) }
    suspend fun getTop25(context: Context) = StationRepository.getTop25(context).map { applyUserOverride(it) }

    // ── קטגוריות resolved ────────────────────────────────────────────

    fun resolveCategories(): List<ResolvedCategory> {
        val base      = StationRepository.getCategoryNames()
        val overrides = userCatalog.nodeOverrides.filter { it.type == "category" }
        val resolved  = base.mapIndexed { idx, name ->
            val ov = overrides.find { it.id == name }
            if (ov?.isDeleted == true) null
            else ResolvedCategory(name, ov?.nameOverride ?: name, ov?.orderIndex ?: idx)
        }.filterNotNull().toMutableList()
        overrides.filter { it.isCustom && !it.isDeleted }.forEach { node ->
            if (resolved.none { it.id == node.id })
                resolved.add(ResolvedCategory(node.id, node.nameOverride ?: node.id, node.orderIndex, true))
        }
        return resolved.sortedBy { it.orderIndex }
    }

    // ── פריטי קטגוריה resolved ───────────────────────────────────────

    fun resolveCategoryItems(categoryId: String): List<ResolvedNode> {
        val base      = StationRepository.getCategoryItems(categoryId) ?: emptyList()
        val overrides = userCatalog.categoryItemOverrides.filter { it.categoryId == categoryId }

        val resolved: MutableList<ResolvedNode> = base.mapIndexed { idx, item ->
            val (itemId, name, type) = when (item) {
                is ListItem.StationItem    -> Triple(item.station.id, item.station.name, NodeType.STATION)
                is ListItem.SubCategoryItem-> Triple("sub_${item.name}", item.name, NodeType.SUBCATEGORY)
                is ListItem.SeparatorItem  -> Triple("sep_${item.title}_$idx", item.title, NodeType.SEPARATOR)
                else                       -> return@mapIndexed null
            }
            val ov = overrides.find { it.itemId == itemId }
            if (ov?.isDeleted == true) return@mapIndexed null
            ResolvedNode(itemId, ov?.nameOverride ?: name, type,
                         ov?.isHidden == true, false, ov?.orderIndex ?: idx)
        }.filterNotNull().toMutableList()

        // הוסף פריטים מותאמים-אישית
        overrides.filter { it.isCustom && !it.isDeleted }.forEach { ov ->
            if (resolved.none { it.id == ov.itemId }) {
                val type = when (ov.itemType) {
                    "subcategory" -> NodeType.SUBCATEGORY
                    "separator"   -> NodeType.SEPARATOR
                    else          -> NodeType.STATION
                }
                resolved.add(ResolvedNode(ov.itemId, ov.nameOverride ?: ov.itemId,
                                          type, ov.isHidden, true, ov.orderIndex))
            }
        }
        return resolved.sortedBy { it.orderIndex }
    }

    // ── עריכת קטגוריות ───────────────────────────────────────────────

    fun renameCategory(context: Context, id: String, newName: String)  = upsertCategoryNode(context, id) { copy(nameOverride = newName) }
    fun deleteCategory(context: Context, id: String)                   = upsertCategoryNode(context, id) { copy(isDeleted = true)  }
    fun restoreCategory(context: Context, id: String)                  = upsertCategoryNode(context, id) { copy(isDeleted = false) }
    fun reorderCategories(context: Context, newOrder: List<String>) {
        val base = userCatalog.nodeOverrides.filter { it.type != "category" }.toMutableList()
        newOrder.forEachIndexed { idx, id ->
            val ex = userCatalog.nodeOverrides.find { it.id == id && it.type == "category" }
            base.add(ex?.copy(orderIndex = idx) ?: UserNode(id, "category", orderIndex = idx))
        }
        saveUserCatalog(context, userCatalog.copy(nodeOverrides = base))
    }
    fun addCategory(context: Context, name: String): String {
        val id  = "custom_${UUID.randomUUID()}"
        val idx = resolveCategories().size
        saveUserCatalog(context, userCatalog.copy(nodeOverrides =
            userCatalog.nodeOverrides + UserNode(id, "category", name, idx, null, false, true)))
        return id
    }

    // ── עריכת פריטים בתוך קטגוריה ───────────────────────────────────

    fun renameCategoryItem(context: Context, catId: String, itemId: String, newName: String) =
        upsertCategoryItem(context, catId, itemId, "station") { copy(nameOverride = newName) }

    fun hideCategoryItem(context: Context, catId: String, itemId: String) =
        upsertCategoryItem(context, catId, itemId, "station") { copy(isHidden = true) }

    fun restoreCategoryItem(context: Context, catId: String, itemId: String) =
        upsertCategoryItem(context, catId, itemId, "station") { copy(isHidden = false) }

    fun deleteCategoryItem(context: Context, catId: String, itemId: String) {
        val isCustom = resolveCategoryItems(catId).find { it.id == itemId }?.isCustom == true
        if (isCustom) {
            saveUserCatalog(context, userCatalog.copy(categoryItemOverrides =
                userCatalog.categoryItemOverrides.filter {
                    !(it.categoryId == catId && it.itemId == itemId) }))
            // אם custom station — מחק גם מ-stationOverrides
            if (userCatalog.stationOverrides[itemId]?.isCustom == true) {
                saveUserCatalog(context, userCatalog.copy(
                    stationOverrides = userCatalog.stationOverrides - itemId))
            }
        } else {
            upsertCategoryItem(context, catId, itemId, "station") { copy(isDeleted = true) }
        }
    }

    fun reorderCategoryItems(context: Context, catId: String, newOrder: List<String>) {
        val base     = userCatalog.categoryItemOverrides.filter { it.categoryId != catId }.toMutableList()
        val existing = userCatalog.categoryItemOverrides.filter { it.categoryId == catId }
        newOrder.forEachIndexed { idx, itemId ->
            val ov = existing.find { it.itemId == itemId }
            base.add(ov?.copy(orderIndex = idx)
                ?: CategoryItemOverride(catId, itemId, "station", orderIndex = idx))
        }
        saveUserCatalog(context, userCatalog.copy(categoryItemOverrides = base))
    }

    fun addStationToCategory(context: Context, catId: String, stationId: String) {
        val idx   = resolveCategoryItems(catId).size
        val items = userCatalog.categoryItemOverrides.toMutableList()
        val ex    = items.indexOfFirst { it.categoryId == catId && it.itemId == stationId }
        if (ex >= 0) items[ex] = items[ex].copy(isHidden = false, isDeleted = false)
        else items.add(CategoryItemOverride(catId, stationId, "station", orderIndex = idx, isCustom = true))
        saveUserCatalog(context, userCatalog.copy(categoryItemOverrides = items))
    }

    fun addSubcategory(context: Context, catId: String, name: String): String {
        val id  = "sub_custom_${UUID.randomUUID()}"
        val idx = resolveCategoryItems(catId).size
        upsertCategoryItem(context, catId, id, "subcategory") {
            copy(nameOverride = name, orderIndex = idx, isCustom = true) }
        return id
    }

    fun addSeparator(context: Context, catId: String, text: String): String {
        val id  = "sep_custom_${UUID.randomUUID()}"
        val idx = resolveCategoryItems(catId).size
        upsertCategoryItem(context, catId, id, "separator") {
            copy(nameOverride = text, orderIndex = idx, isCustom = true) }
        return id
    }

    // ── עריכת תחנות ──────────────────────────────────────────────────

    fun overrideStation(context: Context, stationId: String, ov: UserStation) {
        val map = userCatalog.stationOverrides.toMutableMap()
        if (ov.nameOverride == null && ov.streamUrlOverride == null && ov.logoUrlOverride == null && !ov.isCustom)
            map.remove(stationId)
        else map[stationId] = ov
        saveUserCatalog(context, userCatalog.copy(stationOverrides = map))
    }

    fun addCustomStation(context: Context, catId: String,
                         name: String, streamUrl: String, logoUrl: String?): String {
        val id = "custom_${UUID.randomUUID()}"
        val newMap = userCatalog.stationOverrides.toMutableMap()
        newMap[id]  = UserStation(id, name, streamUrl, logoUrl, isCustom = true)
        val idx     = resolveCategoryItems(catId).size
        val newItems = userCatalog.categoryItemOverrides.toMutableList()
        newItems.add(CategoryItemOverride(catId, id, "station", name, idx, isCustom = true))
        saveUserCatalog(context, userCatalog.copy(stationOverrides = newMap, categoryItemOverrides = newItems))
        return id
    }

    // ── שמירה ────────────────────────────────────────────────────────

    fun saveUserCatalog(context: Context, catalog: UserCatalog) {
        userCatalog = catalog
        UserCatalogStore.save(context, catalog)
    }

    fun resetToDefault(context: Context) {
        userCatalog = UserCatalog()
        UserCatalogStore.reset(context)
    }

    // ── עזרים פנימיים ─────────────────────────────────────────────────

    private fun isHidden(id: String) = userCatalog.stationOverrides[id]?.isHidden == true

    private fun applyUserOverride(station: Station): Station {
        val ov = userCatalog.stationOverrides[station.id] ?: return station
        return station.copy(name = ov.nameOverride ?: station.name,
                            streamUrl = ov.streamUrlOverride ?: station.streamUrl,
                            logoUrl   = ov.logoUrlOverride   ?: station.logoUrl)
    }

    private fun upsertCategoryNode(context: Context, id: String,
                                   transform: UserNode.() -> UserNode) {
        val nodes = userCatalog.nodeOverrides.toMutableList()
        val idx   = nodes.indexOfFirst { it.id == id && it.type == "category" }
        val base  = if (idx >= 0) nodes[idx] else UserNode(id, "category")
        val upd   = base.transform()
        if (idx >= 0) nodes[idx] = upd else nodes.add(upd)
        saveUserCatalog(context, userCatalog.copy(nodeOverrides = nodes))
    }

    private fun upsertCategoryItem(context: Context, catId: String, itemId: String,
                                   itemType: String, transform: CategoryItemOverride.() -> CategoryItemOverride) {
        val items = userCatalog.categoryItemOverrides.toMutableList()
        val idx   = items.indexOfFirst { it.categoryId == catId && it.itemId == itemId }
        val base  = if (idx >= 0) items[idx] else CategoryItemOverride(catId, itemId, itemType)
        val upd   = base.transform()
        if (idx >= 0) items[idx] = upd else items.add(upd)
        saveUserCatalog(context, userCatalog.copy(categoryItemOverrides = items))
    }
}
