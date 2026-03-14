package il.co.radioapp.model

import com.google.gson.annotations.SerializedName

data class Station(
    @SerializedName("id")         val id: String,
    @SerializedName("name")       val name: String,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("logo_url")   val logoUrl: String? = null,
    @SerializedName("page_url")   val pageUrl: String? = null,
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("source")     val source: String = "rlive"
) {
    val isKcm get() = source == "kcm"
}

data class KcmChannel(
    @SerializedName("id")          val id: String,
    @SerializedName("name")        val name: String,
    @SerializedName("stream_url")  val streamUrl: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("logo_url")    val logoUrl: String? = null,
    @SerializedName("categories")  val categories: List<String> = emptyList(),
    @SerializedName("source")      val source: String = "kcm"
) {
    fun toStation() = Station(
        id          = id,
        name        = name,
        streamUrl   = streamUrl,
        logoUrl     = logoUrl,
        categories  = listOf("\u05E2\u05E8\u05D5\u05E6\u05D9 \u05E7\u05D5\u05DC \u05D7\u05D9 \u05DE\u05D9\u05D5\u05D6\u05D9\u05E7"),
        source      = "kcm"
    )
}

data class RadioData(
    @SerializedName("stations")     val stations: List<Station>                      = emptyList(),
    @SerializedName("by_category")  val categories: Map<String, List<CategoryRef>>  = emptyMap(),
    @SerializedName("kcm_channels") val kcmChannels: List<KcmChannel>               = emptyList(),
    @SerializedName("top25_ids")    val top25Ids: List<String>                       = emptyList()
)

data class CategoryRef(@SerializedName("id") val id: String)



