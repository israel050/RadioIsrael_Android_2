package il.co.radioapp.model

sealed class ListItem {
    data class StationItem(val station: Station) : ListItem()
    data class SeparatorItem(val title: String) : ListItem()
    data class SubCategoryItem(val id: String, val name: String, val items: List<ListItem>) : ListItem()
}
