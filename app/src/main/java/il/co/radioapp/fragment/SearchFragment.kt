package il.co.radioapp.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import il.co.radioapp.MainActivity
import il.co.radioapp.SearchActivity
import android.content.Intent

/**
 * Legacy stub – replaced by SearchActivity.
 * If somehow this fragment is still instantiated, immediately launch SearchActivity.
 */
class SearchFragment : Fragment() {
    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        View(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Redirect to SearchActivity and pop self
        parentFragmentManager.popBackStack()
        startActivity(Intent(requireContext(), SearchActivity::class.java))
    }
}



