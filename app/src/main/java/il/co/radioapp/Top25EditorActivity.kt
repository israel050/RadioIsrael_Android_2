package il.co.radioapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.adapter.AddableStationAdapter
import il.co.radioapp.adapter.SelectedStationAdapter
import il.co.radioapp.databinding.ActivityTop25EditorBinding
import il.co.radioapp.model.Station
import il.co.radioapp.repository.StationRepository
import kotlinx.coroutines.launch

class Top25EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTop25EditorBinding
    private lateinit var selectedAdapter: SelectedStationAdapter
    private lateinit var addableAdapter: AddableStationAdapter
    private lateinit var touchHelper: ItemTouchHelper
    private var allStations: List<Station> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTop25EditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        setupSelectedAdapter()
        setupAddableAdapter()
        setupSearch()
        setupTouchHelper()
        loadData()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveAndFinish() }
    }

    private fun setupSelectedAdapter() {
        selectedAdapter = SelectedStationAdapter(
            onRemove = { pos ->
                val removed = selectedAdapter.stations.removeAt(pos)
                selectedAdapter.notifyItemRemoved(pos)
                updateCounts()
                refreshAddable(binding.etSearch.text?.toString() ?: "")
            },
            onStartDrag = { vh -> touchHelper.startDrag(vh) }
        )
        binding.rvSelected.adapter = selectedAdapter
    }

    private fun setupAddableAdapter() {
        addableAdapter = AddableStationAdapter { station ->
            selectedAdapter.stations.add(station)
            selectedAdapter.notifyItemInserted(selectedAdapter.stations.size - 1)
            updateCounts()
            refreshAddable(binding.etSearch.text?.toString() ?: "")
        }
        binding.rvAddable.adapter = addableAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                binding.btnClearSearch.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                refreshAddable(q)
            }
        })
        binding.btnClearSearch.setOnClickListener { binding.etSearch.text.clear() }
    }

    private fun setupTouchHelper() {
        touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                selectedAdapter.moveItem(vh.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun isLongPressDragEnabled() = false   // manual via drag handle only
        })
        touchHelper.attachToRecyclerView(binding.rvSelected)
    }

    private fun loadData() {
        lifecycleScope.launch {
            StationRepository.init(applicationContext)
            allStations = StationRepository.getAllStations().sortedBy { it.name }

            // Load saved or default top25
            val savedIds = AppPreferences.getTop25Ids(applicationContext)
            val initialIds = savedIds.ifEmpty { StationRepository.top25Ids }
            val stationMap = allStations.associateBy { it.id }
            val selected = initialIds.mapNotNull { stationMap[it] }

            selectedAdapter.setData(selected)
            updateCounts()
            refreshAddable("")
        }
    }

    private fun refreshAddable(query: String) {
        val selectedIds = selectedAdapter.stations.map { it.id }.toSet()
        val q = query.trim().lowercase()
        val available = allStations.filter { s ->
            s.id !in selectedIds &&
                (q.isEmpty() || s.name.lowercase().contains(q))
        }
        addableAdapter.submitList(available)
    }

    private fun updateCounts() {
        binding.tvSelectedCount.text = "נבחרות (${selectedAdapter.stations.size})"
    }

    private fun saveAndFinish() {
        val ids = selectedAdapter.stations.map { it.id }
        AppPreferences.setTop25Ids(applicationContext, ids)
        setResult(RESULT_OK)
        finish()
    }
}



