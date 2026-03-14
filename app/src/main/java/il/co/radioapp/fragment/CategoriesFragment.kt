package il.co.radioapp.fragment

import android.os.Bundle; import android.view.*
import androidx.fragment.app.Fragment; import androidx.lifecycle.lifecycleScope
import il.co.radioapp.MainActivity; import il.co.radioapp.R
import il.co.radioapp.adapter.CategoryAdapter
import il.co.radioapp.databinding.FragmentCategoriesBinding
import il.co.radioapp.repository.CatalogRepository; import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): android.view.View {
        _binding = FragmentCategoriesBinding.inflate(inf, c, false); return binding.root
    }

    override fun onViewCreated(view: android.view.View, saved: Bundle?) {
        lifecycleScope.launch {
            CatalogRepository.init(requireContext())
            if (_binding == null) return@launch
            binding.rvCategories.adapter = CategoryAdapter(CatalogRepository.getCategoryNames()) { cat ->
                val act = activity as? MainActivity ?: return@CategoryAdapter
                act.showFragmentContainer()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, CategoryStationsFragment.newInstance(cat))
                    .addToBackStack(null).commit()
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
