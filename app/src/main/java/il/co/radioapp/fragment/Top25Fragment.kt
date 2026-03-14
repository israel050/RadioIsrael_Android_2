package il.co.radioapp.fragment

import android.os.Bundle; import android.view.*
import androidx.fragment.app.Fragment; import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import il.co.radioapp.adapter.StationAdapter
import il.co.radioapp.databinding.FragmentStationListBinding
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.viewmodel.PlayerViewModel; import kotlinx.coroutines.launch

class Top25Fragment : Fragment() {
    private var _binding: FragmentStationListBinding? = null
    private val binding get() = _binding!!
    private val vm: PlayerViewModel by activityViewModels()

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): android.view.View {
        _binding = FragmentStationListBinding.inflate(inf, c, false); return binding.root
    }
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) = loadTop25()
    override fun onResume() { super.onResume(); loadTop25() }

    private fun loadTop25() {
        lifecycleScope.launch {
            CatalogRepository.init(requireContext())
            if (_binding == null) return@launch
            val stations = CatalogRepository.getTop25(requireContext())
            binding.rvStations.adapter = StationAdapter(stations, vm) { station ->
                vm.playStation(station, stations)
                (requireActivity() as il.co.radioapp.MainActivity).openPlayer(station)
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
