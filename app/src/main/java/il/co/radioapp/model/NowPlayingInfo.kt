package il.co.radioapp.model

data class NowPlayingInfo(
    val artist: String?,   // null when stream doesn't separate artist from title
    val title: String
)

fun parseStreamTitle(raw: String): NowPlayingInfo? {
    val trimmed = raw.trim().trimEnd('\u0000')
    if (trimmed.isBlank()) return null
    return if (trimmed.contains(" - ")) {
        val idx = trimmed.indexOf(" - ")
        NowPlayingInfo(
            artist = trimmed.substring(0, idx).trim().ifBlank { null },
            title  = trimmed.substring(idx + 3).trim()
        )
    } else {
        NowPlayingInfo(artist = null, title = trimmed)
    }
}



