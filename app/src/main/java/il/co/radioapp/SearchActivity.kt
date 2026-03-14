package il.co.radioapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import il.co.radioapp.databinding.ActivitySearchBinding
import il.co.radioapp.model.Station
import il.co.radioapp.adapter.SearchStationAdapter
import il.co.radioapp.repository.StationRepository
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    companion object {
        const val RESULT_STATION_ID   = "station_id"
        const val RESULT_STATION_NAME = "station_name"
        const val RESULT_STATION_URL  = "station_url"
        const val RESULT_STATION_LOGO = "station_logo"
    }

    private lateinit var binding: ActivitySearchBinding
    private var allStations: List<Station> = emptyList()
    private var filteredStations: List<Station> = emptyList()
    private lateinit var adapter: SearchStationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        setupAdapter()
        setupSearch()
        loadStations()
    }

    private fun setupAdapter() {
        adapter = SearchStationAdapter { station ->
            // Return selected station to MainActivity
            val data = Intent().apply {
                putExtra(RESULT_STATION_ID,   station.id)
                putExtra(RESULT_STATION_NAME, station.name)
                putExtra(RESULT_STATION_URL,  station.streamUrl)
                putExtra(RESULT_STATION_LOGO, station.logoUrl ?: "")
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        binding.rvResults.adapter = adapter
    }

    private fun setupSearch() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
            binding.etSearch.requestFocus()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.btnClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterStations(query)
            }
        })

        // Open keyboard automatically
        binding.etSearch.requestFocus()
        binding.etSearch.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun loadStations() {
        lifecycleScope.launch {
            StationRepository.init(applicationContext)
            allStations = StationRepository.getAllStations()
                .sortedBy { it.name }  // alphabetical
            filterStations("")  // show all
        }
    }

    private fun filterStations(query: String) {
        val q = query.trim()
        filteredStations = if (q.isEmpty()) {
            allStations
        } else {
            val lower = q.lowercase()
            allStations.filter { station ->
                station.name.lowercase().contains(lower)
            }
        }
        adapter.submitList(filteredStations)
        updateResultCount(query, filteredStations.size, allStations.size)
    }

    private fun updateResultCount(query: String, found: Int, total: Int) {
        binding.tvResultCount.text = when {
            query.isEmpty() -> "כל התחנות ($total)"
            found == 0      -> "לא נמצאו תחנות"
            else            -> "נמצאו $found תחנות"
        }
    }
}



