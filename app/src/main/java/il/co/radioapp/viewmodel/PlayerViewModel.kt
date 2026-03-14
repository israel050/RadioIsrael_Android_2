package il.co.radioapp.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import il.co.radioapp.model.NowPlayingInfo
import il.co.radioapp.model.parseStreamTitle
import il.co.radioapp.model.Station
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.repository.FavoritesRepository
import il.co.radioapp.repository.PlaybackQueue
import il.co.radioapp.service.RadioPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Future
import android.os.Bundle

enum class PlaybackStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context = app.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var controllerFuture: Future<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingStation: Pair<Station, List<Station>>? = null

    private val _currentStation  = MutableLiveData<Station?>()
    val currentStation: LiveData<Station?> = _currentStation

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _playbackStatus = MutableLiveData(PlaybackStatus.IDLE)
    val playbackStatus: LiveData<PlaybackStatus> = _playbackStatus

    private val _playbackError = MutableLiveData<String?>(null)
    val playbackError: LiveData<String?> = _playbackError

    private val _volume = MutableLiveData(FavoritesRepository.getVolume(ctx))
    val volume: LiveData<Float> = _volume

    private val _favorites = MutableLiveData<Set<String>>(FavoritesRepository.getFavoriteIds(ctx))
    val favorites: LiveData<Set<String>> = _favorites

    // nowPlayingInfo as StateFlow (PlayerActivity collects via Flow)
    private val _nowPlayingInfo = MutableStateFlow<NowPlayingInfo?>(null)
    val nowPlayingInfo: StateFlow<NowPlayingInfo?> = _nowPlayingInfo

    private var lastStreamUrl   = ""
    private var lastStationId   = ""
    private var lastStationName = ""

    // ── Now-Playing BroadcastReceiver (lifecycle-managed by PlayerActivity) ──
    private var nowPlayingReceiver: BroadcastReceiver? = null

    fun registerNowPlayingReceiver(context: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    RadioPlayerService.ACTION_NOW_PLAYING -> {
                        val raw = intent.getStringExtra(RadioPlayerService.EXTRA_STREAM_TITLE) ?: return
                        val info = parseStreamTitle(raw) ?: return
                        if (info.title != lastStationName) _nowPlayingInfo.value = info
                    }
                    RadioPlayerService.ACTION_PLAYBACK_ERROR -> {
                        val msg = intent.getStringExtra(RadioPlayerService.EXTRA_ERROR_MSG) ?: return
                        _playbackError.postValue(msg)
                        _playbackStatus.postValue(PlaybackStatus.ERROR)
                        _isPlaying.postValue(false)
                    }
                }
            }
        }
        nowPlayingReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(RadioPlayerService.ACTION_NOW_PLAYING)
            addAction(RadioPlayerService.ACTION_PLAYBACK_ERROR)
        }
        ContextCompat.registerReceiver(context, receiver, filter,
            ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun unregisterNowPlayingReceiver(context: Context) {
        nowPlayingReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
            nowPlayingReceiver = null
        }
    }

    // ── MainActivity receiver (always-on for mini-player error dot) ──
    private val globalReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == RadioPlayerService.ACTION_PLAYBACK_ERROR) {
                val msg = intent.getStringExtra(RadioPlayerService.EXTRA_ERROR_MSG) ?: return
                _playbackError.postValue(msg)
                _playbackStatus.postValue(PlaybackStatus.ERROR)
                _isPlaying.postValue(false)
            }
        }
    }

    init {
        val filter = IntentFilter(RadioPlayerService.ACTION_PLAYBACK_ERROR)
        ContextCompat.registerReceiver(ctx, globalReceiver, filter,
            ContextCompat.RECEIVER_NOT_EXPORTED)
        viewModelScope.launch { CatalogRepository.init(ctx) }
        connectToService()
    }

    fun resetNowPlaying(streamUrl: String, stationId: String) {
        lastStreamUrl = streamUrl; lastStationId = stationId; _nowPlayingInfo.value = null
    }

    /** Called by MainActivity's MEDIA_KEY receiver to sync station without re-preparing */
    fun syncStation(station: Station) {
        _currentStation.postValue(station)
        resetNowPlaying(station.streamUrl, station.id)
        lastStationName = station.name
    }

    /** Re-reads favorites from SharedPreferences (called after returning from FavoritesFragment) */
    fun refreshFavorites() {
        _favorites.value = FavoritesRepository.getFavoriteIds(ctx)
    }

    private fun connectToService() {
        val token = SessionToken(ctx, ComponentName(ctx, RadioPlayerService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val mc = future.get() ?: return@addListener
                controller = mc
                mc.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.postValue(playing)
                        if (playing) {
                            _playbackStatus.postValue(PlaybackStatus.PLAYING)
                            _playbackError.postValue(null)
                        } else if (_playbackStatus.value == PlaybackStatus.PLAYING) {
                            _playbackStatus.postValue(PlaybackStatus.PAUSED)
                        }
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_BUFFERING)
                            _playbackStatus.postValue(PlaybackStatus.LOADING)
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        val msg = when {
                            error.message?.contains("418")    == true -> "🚫 הזרמה חסומה בסינון"
                            error.message?.contains("host")   == true ||
                            error.message?.contains("resolv") == true -> "⚠️ אין חיבור לאינטרנט"
                            error.message?.contains("SSL")    == true ||
                            error.message?.contains("cert")   == true -> "🔒 שגיאת אבטחה"
                            error.message?.contains("404")    == true -> "❌ תחנה לא נמצאה"
                            else -> "❌ שגיאת שרת (${error.errorCodeName})"
                        }
                        _playbackError.postValue(msg)
                        _playbackStatus.postValue(PlaybackStatus.ERROR)
                        _isPlaying.postValue(false)
                    }
                })
                _volume.value?.let { setVolume(it) }
                pendingStation?.let { (s, list) -> pendingStation = null; playStation(s, list) }
            } catch (e: Exception) { /* service not ready */ }
        }, mainHandler::post)
    }

    fun playStation(station: Station, list: List<Station> = emptyList()) {
        _currentStation.value = station
        _playbackStatus.value = PlaybackStatus.LOADING
        _playbackError.value  = null
        lastStationName       = station.name
        resetNowPlaying(station.streamUrl, station.id)
        PlaybackQueue.update(list.ifEmpty { listOf(station) }, station)
        FavoritesRepository.saveLastStation(ctx, station.id)
        val c = controller
        if (c == null) { pendingStation = Pair(station, list); connectToService(); return }
        val args = Bundle().apply {
            putString(RadioPlayerService.ARG_URL,   station.streamUrl)
            putString(RadioPlayerService.ARG_TITLE, station.name)
            putString(RadioPlayerService.ARG_LOGO,  station.logoUrl ?: "")
        }
        c.sendCustomCommand(SessionCommand(RadioPlayerService.CMD_PLAY_URL, Bundle.EMPTY), args)
    }

    fun togglePlayPause() { val c = controller ?: return; if (c.isPlaying) c.pause() else c.play() }
    fun playNext() { val next = PlaybackQueue.next() ?: return; playStation(next, PlaybackQueue.stations) }
    fun playPrev() { val prev = PlaybackQueue.prev() ?: return; playStation(prev, PlaybackQueue.stations) }
    fun setVolume(v: Float) { controller?.volume = v; _volume.value = v; FavoritesRepository.saveVolume(ctx, v) }
    fun isFavorite(id: String) = _favorites.value?.contains(id) == true
    fun toggleFavorite(id: String) {
        val cur = _favorites.value?.toMutableSet() ?: mutableSetOf()
        if (!cur.remove(id)) cur.add(id)
        _favorites.value = cur
        // FavoritesRepository stores individually via toggleFavorite(ctx, id)
        FavoritesRepository.toggleFavorite(ctx, id)
        _favorites.value = FavoritesRepository.getFavoriteIds(ctx) // sync back
    }
    fun resumeOrPlayDefault() {
        if (controller?.isPlaying == true) return
        val id      = FavoritesRepository.getLastStationId(ctx)
        val station = id?.let { CatalogRepository.getStation(it) }
                   ?: CatalogRepository.getTop25().firstOrNull() ?: return
        playStation(station, CatalogRepository.getTop25())
    }

    override fun onCleared() {
        super.onCleared()
        try { ctx.unregisterReceiver(globalReceiver) } catch (_: Exception) {}
        controllerFuture?.cancel(false)
        controller?.release()
    }
}
