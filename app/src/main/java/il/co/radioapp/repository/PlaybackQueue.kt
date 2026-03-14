package il.co.radioapp.repository

import il.co.radioapp.model.Station

/**
 * Singleton that holds the current playback list and position.
 * Written by the shared PlayerViewModel (MainActivity scope),
 * read by PlayerActivity's local ViewModel for prev/next navigation.
 */
object PlaybackQueue {
    var stations: List<Station> = emptyList()
    var currentIndex: Int = -1

    fun update(list: List<Station>, station: Station) {
        stations = list.ifEmpty { listOf(station) }
        currentIndex = stations.indexOfFirst { it.id == station.id }
    }

    fun next(): Station? {
        if (stations.isEmpty()) return null
        currentIndex = (currentIndex + 1) % stations.size
        return stations[currentIndex]
    }

    fun prev(): Station? {
        if (stations.isEmpty()) return null
        currentIndex = (currentIndex - 1 + stations.size) % stations.size
        return stations[currentIndex]
    }
}



