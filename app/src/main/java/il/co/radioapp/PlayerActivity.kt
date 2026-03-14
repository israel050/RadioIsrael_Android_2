package il.co.radioapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import il.co.radioapp.databinding.ActivityPlayerBinding
import il.co.radioapp.model.Station
import il.co.radioapp.repository.PlaybackQueue
import il.co.radioapp.repository.StationRepository
import il.co.radioapp.util.AssetImageUtil
import il.co.radioapp.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STATION_ID   = "station_id"
        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_STATION_URL  = "station_url"
        const val EXTRA_STATION_LOGO = "station_logo"
    }

    private lateinit var binding: ActivityPlayerBinding
    private val vm: PlayerViewModel by viewModels()
    private lateinit var station: Station

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val id = intent.getStringExtra(EXTRA_STATION_ID) ?: run { finish(); return }

        station = StationRepository.getStation(id) ?: run {
            val name = intent.getStringExtra(EXTRA_STATION_NAME) ?: run { finish(); return }
            val url  = intent.getStringExtra(EXTRA_STATION_URL)  ?: run { finish(); return }
            val logo = intent.getStringExtra(EXTRA_STATION_LOGO) ?: ""
            Station(id = id, name = name, streamUrl = url, logoUrl = logo.ifEmpty { null }, source = "kcm")
        }

        updateUI(station)
        vm.resetNowPlaying(station.streamUrl, station.id)  // start poll for this station
        observeViewModel()
        observeNowPlaying()
        setupControls()
    }

    override fun onStart() {
        super.onStart()
        vm.registerNowPlayingReceiver(this)
    }

    override fun onStop() {
        super.onStop()
        vm.unregisterNowPlayingReceiver(this)
    }

    private fun updateUI(s: Station) {
        binding.tvStationName.text = s.name
        AssetImageUtil.loadLogoSquare(this, s.id, s.logoUrl, binding.ivLogo)
        updateFavoriteIcon()
        // Reset now-playing display immediately when station changes
        binding.llNowPlaying.visibility = View.GONE
    }

    private fun observeViewModel() {
        // ── Error Banner ──────────────────────────────────────────
        vm.playbackError.observe(this) { msg ->
            if (msg != null) {
                binding.tvErrorBanner.text       = msg
                binding.tvErrorBanner.visibility = android.view.View.VISIBLE
            } else {
                binding.tvErrorBanner.visibility = android.view.View.GONE
            }
        }

        vm.isPlaying.observe(this) { playing ->
            binding.btnPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        }
        vm.volume.observe(this) { binding.seekVolume.progress = (it * 100).toInt() }
        vm.currentStation.observe(this) { s ->
            if (s != null && s.id != station.id) { station = s; updateUI(s) }
        }
        vm.favorites.observe(this) { updateFavoriteIcon() }
    }

    private fun observeNowPlaying() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.nowPlayingInfo.collect { info ->
                    if (info != null) {
                        binding.llNowPlaying.visibility = View.VISIBLE
                        if (!info.artist.isNullOrBlank()) {
                            binding.tvArtistName.visibility = View.VISIBLE
                            binding.tvArtistName.text = info.artist
                            binding.tvArtistName.isSelected = true  // activate marquee
                        } else {
                            binding.tvArtistName.visibility = View.GONE
                        }
                        binding.tvSongTitle.text = info.title
                        binding.tvSongTitle.isSelected = true       // activate marquee
                    } else {
                        binding.llNowPlaying.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        val isFav = vm.isFavorite(station.id)
        binding.btnFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        binding.btnFavorite.imageTintList = ContextCompat.getColorStateList(this,
            if (isFav) R.color.heartActive else R.color.heartInactive)
    }

    private fun setupControls() {
        binding.btnMinimize.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnPlayPause.setOnClickListener { vm.togglePlayPause() }
        binding.btnNext.setOnClickListener { navigateNext() }
        binding.btnPrev.setOnClickListener { navigatePrev() }
        binding.btnFavorite.setOnClickListener {
            vm.toggleFavorite(station.id)
            setResult(RESULT_OK)
        }
        binding.seekVolume.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, p: Int, byUser: Boolean) {
                if (byUser) vm.setVolume(p / 100f)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })
    }

    private fun navigateNext() {
        val next = PlaybackQueue.next() ?: return
        station = next
        vm.playStation(next, PlaybackQueue.stations)
        updateUI(next)
    }

    private fun navigatePrev() {
        val prev = PlaybackQueue.prev() ?: return
        station = prev
        vm.playStation(prev, PlaybackQueue.stations)
        updateUI(prev)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_MEDIA_NEXT         -> { navigateNext(); true }
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS     -> { navigatePrev(); true }
        KeyEvent.KEYCODE_DPAD_UP            -> { adjustVol(0.05f);     true }
        KeyEvent.KEYCODE_DPAD_DOWN          -> { adjustVol(-0.05f);    true }
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_1                  -> { vm.togglePlayPause(); true }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun adjustVol(d: Float) = vm.setVolume(((vm.volume.value ?: 1f) + d).coerceIn(0f, 1f))

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}



