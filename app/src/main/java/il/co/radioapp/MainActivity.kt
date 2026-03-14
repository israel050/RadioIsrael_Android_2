
package il.co.radioapp

import android.content.BroadcastReceiver
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import il.co.radioapp.util.AssetImageUtil
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import il.co.radioapp.databinding.ActivityMainBinding
import il.co.radioapp.fragment.CategoriesFragment
import il.co.radioapp.fragment.FavoritesFragment
import il.co.radioapp.fragment.Top25Fragment
import il.co.radioapp.viewmodel.PlayerViewModel
import androidx.viewpager2.adapter.FragmentStateAdapter
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val playerViewModel: PlayerViewModel by viewModels()

    private val searchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val d = result.data ?: return@registerForActivityResult
            val id   = d.getStringExtra(SearchActivity.RESULT_STATION_ID)   ?: return@registerForActivityResult
            val name = d.getStringExtra(SearchActivity.RESULT_STATION_NAME) ?: return@registerForActivityResult
            val url  = d.getStringExtra(SearchActivity.RESULT_STATION_URL)  ?: return@registerForActivityResult
            val logo = d.getStringExtra(SearchActivity.RESULT_STATION_LOGO) ?: ""
            val station = il.co.radioapp.repository.StationRepository.getStation(id)
                ?: il.co.radioapp.model.Station(id, name, url, logo.ifEmpty { null }, source = "kcm")
            // Play in context of all stations (search context)
            val list = il.co.radioapp.repository.StationRepository.getAllStations()
            playerViewModel.playStation(station, list)
            openPlayer(station)
        }
    }


    private val top25Launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Top25Fragment will refresh on onResume
        }
    }

    private val mediaKeyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val action = intent.getStringExtra("action") ?: return
            val stationId = intent.getStringExtra("station_id")
            when (action) {
                il.co.radioapp.service.RadioPlayerService.ACTION_NEXT -> {
                    // If service already navigated (stationId provided), just sync UI
                    if (stationId != null) {
                        val s = il.co.radioapp.repository.StationRepository.getStation(stationId)
                        if (s != null) { playerViewModel.syncStation(s); return }
                    }
                    playerViewModel.playNext()
                }
                il.co.radioapp.service.RadioPlayerService.ACTION_PREV -> {
                    if (stationId != null) {
                        val s = il.co.radioapp.repository.StationRepository.getStation(stationId)
                        if (s != null) { playerViewModel.syncStation(s); return }
                    }
                    playerViewModel.playPrev()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTabs()
        setupMiniPlayer()
        registerReceiver(mediaKeyReceiver, IntentFilter("il.co.radioapp.MEDIA_KEY"),
            RECEIVER_NOT_EXPORTED)
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
                binding.fragmentContainer.visibility = View.GONE
        }
        binding.btnSearch.setOnClickListener { openSearch() }
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(android.view.Gravity.START) }
        setupDrawer()
    }

    private fun setupDrawer() {
        val drawer = binding.drawerLayout
        val switchTheme = drawer.findViewById<SwitchCompat>(R.id.switchTheme)
        val menuTheme  = drawer.findViewById<View>(R.id.menuTheme)
        val menuTop25  = drawer.findViewById<View>(R.id.menuTop25)
        val menuAbout  = drawer.findViewById<View>(R.id.menuAbout)
        val menuCatalogEditor = drawer.findViewById<View>(R.id.menuCatalogEditor)

        // Init switch state
        switchTheme.isChecked = AppPreferences.getTheme(this) == AppPreferences.THEME_DARK

        menuCatalogEditor?.setOnClickListener {
            drawer.closeDrawers()
            startActivity(android.content.Intent(this, CatalogEditorActivity::class.java))
        }

        menuTheme.setOnClickListener {
            val isDark = !switchTheme.isChecked
            switchTheme.isChecked = isDark
            val theme = if (isDark) AppPreferences.THEME_DARK else AppPreferences.THEME_LIGHT
            AppPreferences.setTheme(this, theme)
            AppPreferences.applyTheme(theme)
            recreate()
        }

        menuTop25.setOnClickListener {
            drawer.closeDrawers()
            top25Launcher.launch(Intent(this, Top25EditorActivity::class.java))
        }

        menuAbout.setOnClickListener {
            drawer.closeDrawers()
            AlertDialog.Builder(this)
                .setTitle("אודות")
                .setMessage("רדיו ישראל\n\nפותח על ידי:\nישראל גולדווסר\n\nגרסה 3.0")
                .setPositiveButton("סגור", null)
                .show()
        }
    }

    private fun openSearch() {
        searchLauncher.launch(Intent(this, SearchActivity::class.java))
    }

    fun showFragmentContainer() { binding.fragmentContainer.visibility = View.VISIBLE }

    fun clearBackStack() {
        repeat(supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStackImmediate()
        }
        binding.fragmentContainer.visibility = View.GONE
    }

    private fun setupTabs() {
        val fragments = listOf(CategoriesFragment(), Top25Fragment(), FavoritesFragment())
        val labels = listOf("קטגוריות",
                            "25 המובילות",
                            "מועדפים")
        val icons = listOf(R.drawable.ic_category, R.drawable.ic_top25, R.drawable.ic_favorite)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(pos: Int): Fragment = fragments[pos]
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = labels[pos]
            tab.setIcon(icons[pos])
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = clearBackStack()
            override fun onTabReselected(tab: TabLayout.Tab) = clearBackStack()
            override fun onTabUnselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupMiniPlayer() {
        playerViewModel.currentStation.observe(this) { station ->
            binding.miniPlayer.root.visibility = if (station == null) View.GONE else View.VISIBLE
            station?.let {
                binding.miniPlayer.tvMiniTitle.text = it.name
                AssetImageUtil.loadLogo(this, it.id, it.logoUrl, binding.miniPlayer.ivMiniLogo)
            }
        }
        playerViewModel.playbackError.observe(this) { err ->
            val dot = binding.miniPlayer.vErrorDot
            dot?.visibility = if (err != null) android.view.View.VISIBLE else android.view.View.GONE
        }

        playerViewModel.isPlaying.observe(this) { playing ->
            binding.miniPlayer.btnMiniPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        binding.miniPlayer.btnMiniPlayPause.setOnClickListener { playerViewModel.togglePlayPause() }
        val goToPlayer = View.OnClickListener {
            playerViewModel.currentStation.value?.let { openPlayer(it) }
        }
        binding.miniPlayer.root.setOnClickListener(goToPlayer)
        binding.miniPlayer.tvMiniTitle.setOnClickListener(goToPlayer)
        binding.miniPlayer.ivMiniLogo.setOnClickListener(goToPlayer)
    }


    fun openPlayer(station: il.co.radioapp.model.Station) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STATION_ID, station.id)
            putExtra(PlayerActivity.EXTRA_STATION_NAME, station.name)
            putExtra(PlayerActivity.EXTRA_STATION_URL, station.streamUrl)
            putExtra(PlayerActivity.EXTRA_STATION_LOGO, station.logoUrl ?: "")
        })
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.refreshFavorites()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()
        else super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP    -> { adjustVolume(0.05f); true }
            KeyEvent.KEYCODE_DPAD_DOWN  -> { adjustVolume(-0.05f); true }
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { playerViewModel.playPrev(); true }
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_NEXT     -> { playerViewModel.playNext(); true }
            KeyEvent.KEYCODE_1          -> { handleKey1(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun adjustVolume(delta: Float) {
        val cur = playerViewModel.volume.value ?: 1f
        playerViewModel.setVolume((cur + delta).coerceIn(0f, 1f))
    }

    private fun handleKey1() {
        if (playerViewModel.currentStation.value != null) playerViewModel.togglePlayPause()
        else playerViewModel.resumeOrPlayDefault()
    }

    override fun onDestroy() { unregisterReceiver(mediaKeyReceiver); super.onDestroy() }
}



