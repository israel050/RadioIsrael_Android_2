package il.co.radioapp.fragment

import android.os.Bundle
import android.view.*
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import il.co.radioapp.MainActivity
import il.co.radioapp.R
import il.co.radioapp.adapter.StationAdapter
import il.co.radioapp.adapter.StationListAdapter
import il.co.radioapp.databinding.FragmentStationListBinding
import il.co.radioapp.model.ListItem
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class CategoryStationsFragment : Fragment() {

    companion object {
        private const val ARG_CATEGORY = "category"
        private const val ARG_SUB_NAME = "sub_name"
        private const val ARG_SUB_IDS  = "sub_ids"

        fun newInstance(category: String) = CategoryStationsFragment().apply {
            arguments = Bundle().apply { putString(ARG_CATEGORY, category) }
        }

        fun newInstanceSub(sub: ListItem.SubCategoryItem) = CategoryStationsFragment().apply {
            val ids = ArrayList(sub.items
                .filterIsInstance<ListItem.StationItem>()
                .map { it.station.id })
            arguments = Bundle().apply {
                putString(ARG_SUB_NAME, sub.name)
                putStringArrayList(ARG_SUB_IDS, ids)
            }
        }
    }

    private var _binding: FragmentStationListBinding? = null
    private val binding get() = _binding!!
    private val vm: PlayerViewModel by activityViewModels()

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentStationListBinding.inflate(inf, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = loadContent()

    private fun loadContent() {
        val category = arguments?.getString(ARG_CATEGORY)
        val subName  = arguments?.getString(ARG_SUB_NAME)
        val subIds   = arguments?.getStringArrayList(ARG_SUB_IDS)

        lifecycleScope.launch {
            CatalogRepository.init(requireContext())
            if (_binding == null) return@launch

            when {
                // ── תת-קטגוריה: הצג כפתור חזרה + תחנות ──────────
                subIds != null -> {
                    binding.layoutTitleRow.visibility = View.VISIBLE
                    binding.dividerTitle.visibility   = View.VISIBLE
                    binding.btnBack.visibility        = View.VISIBLE   // ← תיקון מפורש
                    binding.tvTitle.text              = subName ?: ""
                    binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

                    val stations = subIds.mapNotNull { CatalogRepository.getStation(it) }
                    binding.rvStations.adapter = StationAdapter(stations, vm) { s ->
                        vm.playStation(s, stations)
                        (requireActivity() as MainActivity).openPlayer(s)
                    }
                }

                // ── קטגוריה ראשית: ללא כפתור חזרה ───────────────
                category != null -> {
                    binding.layoutTitleRow.visibility = View.GONE
                    binding.dividerTitle.visibility   = View.GONE
                    binding.btnBack.visibility        = View.GONE

                    val items = CatalogRepository.getCategoryItems(category)
                    val isComplex = items?.any {
                        it is ListItem.SeparatorItem || it is ListItem.SubCategoryItem
                    } == true

                    if (isComplex && items != null) {
                        binding.rvStations.adapter = StationListAdapter(
                            items = items, vm = vm,
                            onStation = { s, list ->
                                vm.playStation(s, list)
                                (requireActivity() as MainActivity).openPlayer(s)
                            },
                            onSubCategory = { sub ->
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragmentContainer, newInstanceSub(sub))
                                    .addToBackStack(null).commit()
                            }
                        )
                    } else {
                        val list = CatalogRepository.getStationsByCategory(category)
                        binding.rvStations.adapter = StationAdapter(list, vm) { s ->
                            vm.playStation(s, list)
                            (requireActivity() as MainActivity).openPlayer(s)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
