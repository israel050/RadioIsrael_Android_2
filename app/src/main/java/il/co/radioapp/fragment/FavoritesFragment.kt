package il.co.radioapp.fragment

import android.os.Bundle; import android.view.*
import android.view.View
import androidx.fragment.app.Fragment; import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import il.co.radioapp.R
import il.co.radioapp.adapter.StationAdapter
import il.co.radioapp.databinding.FragmentStationListBinding
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.viewmodel.PlayerViewModel; import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {
    private var _binding: FragmentStationListBinding? = null
    private val binding get() = _binding!!
    private val vm: PlayerViewModel by activityViewModels()

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentStationListBinding.inflate(inf, c, false); return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            CatalogRepository.init(requireContext())
            if (_binding == null) return@launch
            refreshList()
            vm.favorites.observe(viewLifecycleOwner) { refreshList() }
        }
    }

    private fun refreshList() {
        if (_binding == null) return
        val allStations = CatalogRepository.getAllStations()
        val favIds      = vm.favorites.value ?: emptySet()
        val favStations = allStations.filter { it.id in favIds }

        if (favStations.isEmpty()) {
            binding.tvEmpty.visibility    = View.VISIBLE
            binding.rvStations.visibility = View.GONE
            binding.tvEmpty.text          = getString(R.string.no_favorites)
        } else {
            binding.tvEmpty.visibility    = View.GONE
            binding.rvStations.visibility = View.VISIBLE
            binding.rvStations.adapter    = StationAdapter(favStations, vm) { station ->
                vm.playStation(station, favStations)
                (requireActivity() as il.co.radioapp.MainActivity).openPlayer(station)
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
