package il.co.radioapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.common.ForwardingPlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import il.co.radioapp.MainActivity
import il.co.radioapp.repository.PlaybackQueue

class RadioPlayerService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID   = "radio_playback"
        const val CMD_PLAY_URL = "PLAY_URL"
        const val CMD_SET_VOL  = "SET_VOL"
        const val ACTION_PLAY  = "il.co.radioapp.PLAY"
        const val ACTION_PAUSE = "il.co.radioapp.PAUSE"
        const val ACTION_NEXT  = "il.co.radioapp.NEXT"
        const val ACTION_PREV  = "il.co.radioapp.PREV"
        const val ACTION_STOP  = "il.co.radioapp.STOP"
        const val ARG_URL      = "url"
        const val ARG_TITLE    = "title"
        const val ARG_LOGO     = "logo"
        const val ARG_VOL          = "vol"
        const val ACTION_NOW_PLAYING   = "il.co.radioapp.NOW_PLAYING"
        const val ACTION_PLAYBACK_ERROR = "il.co.radioapp.PLAYBACK_ERROR"
        const val EXTRA_STREAM_TITLE    = "stream_title"
        const val EXTRA_ERROR_MSG       = "error_msg"
    }

    private lateinit var player: ExoPlayer
    private var currentStreamUrl: String = ""
    private var currentStationTitle: String = ""
    private lateinit var mediaSession: MediaSession
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        player = ExoPlayer.Builder(this).build()

        // ── ICY Now-Playing Listener ───────────────────────────────────
        player.addListener(object : androidx.media3.common.Player.Listener {
            // Path A: raw ICY metadata from Shoutcast/Icecast HTTP streams
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    if (entry is IcyInfo && !entry.title.isNullOrBlank()) {
                        broadcastNowPlaying(entry.title!!)
                        return
                    }
                }
            }
            // Path B: ExoPlayer populates MediaMetadata from ICY (Media3 1.1+)
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val icyTitle = mediaMetadata.title?.toString() ?: return
                // Guard: ignore if it's just the station name we set ourselves
                if (icyTitle.isNotBlank()
                        && icyTitle != currentStreamUrl
                        && icyTitle != currentStationTitle) {
                    broadcastNowPlaying(icyTitle)
                }
            }
        })

        // ForwardingPlayer: intercept seekToNext/Prev to navigate our stations
        val radioPlayer = object : ForwardingPlayer(player) {

            override fun getAvailableCommands(): Player.Commands =
                super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

            override fun seekToNextMediaItem() = navigateStation(next = true)
            override fun seekToNext()           = navigateStation(next = true)
            override fun seekToPreviousMediaItem() = navigateStation(next = false)
            override fun seekToPrevious()          = navigateStation(next = false)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, radioPlayer)
            .setCallback(SessionCallback())
            .setSessionActivity(contentIntent)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build()
        )
    }

    /**
     * Navigate to next or previous station.
     * Works even when the Activity is killed – directly updates ExoPlayer.
     * Also broadcasts for UI update if Activity is alive.
     */
    fun broadcastNowPlaying(streamTitle: String) {
        sendBroadcast(Intent(ACTION_NOW_PLAYING).apply {
            putExtra(EXTRA_STREAM_TITLE, streamTitle)
        })
    }

    fun getCurrentStreamUrl(): String = currentStreamUrl

    private fun navigateStation(next: Boolean) {
        val station = (if (next) PlaybackQueue.next() else PlaybackQueue.prev()) ?: return
        mainHandler.post {
            currentStreamUrl = station.streamUrl
            val item = MediaItem.Builder().setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(station.name)
                        .setArtworkUri(station.logoUrl?.let { android.net.Uri.parse(it) })
                        .build()
                ).build()
            player.setMediaItem(item)
            player.prepare()
            player.play()
        }
        sendBroadcast(Intent("il.co.radioapp.MEDIA_KEY").apply {
            putExtra("action", if (next) ACTION_NEXT else ACTION_PREV)
            putExtra("station_id", station.id)
        })
    }

    inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(session: MediaSession,
            controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val cmds = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_PLAY_URL, Bundle.EMPTY))
                .add(SessionCommand(CMD_SET_VOL,  Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                cmds, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
        }

        override fun onCustomCommand(session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_PLAY_URL -> {
                    val url = args.getString(ARG_URL)
                        ?: return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                    val title = args.getString(ARG_TITLE, "")
                    val logo  = args.getString(ARG_LOGO, "")
                    mainHandler.post {
                        val item = MediaItem.Builder().setUri(url)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle(title)
                                .setArtworkUri(
                                    if (logo.isNotEmpty()) android.net.Uri.parse(logo) else null)
                                .build()).build()
                        currentStreamUrl = url
                        currentStationTitle = title
                        player.setMediaItem(item)
                        player.prepare()
                        player.play()
                    }
                }
                CMD_SET_VOL -> mainHandler.post {
                    player.volume = args.getFloat(ARG_VOL, 1.0f)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "ניגון רדיו", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY  -> mainHandler.post { if (this::player.isInitialized) player.play() }
            ACTION_PAUSE -> mainHandler.post { if (this::player.isInitialized) player.pause() }
            ACTION_STOP  -> mainHandler.post {
                if (this::player.isInitialized) player.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onGetSession(c: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (this::mediaSession.isInitialized) mediaSession.release()
        if (this::player.isInitialized) player.release()
        super.onDestroy()
    }
}



