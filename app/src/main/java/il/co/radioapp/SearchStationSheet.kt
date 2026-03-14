package il.co.radioapp

import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil; import androidx.recyclerview.widget.ListAdapter; import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import il.co.radioapp.databinding.FragmentSearchStationBinding
import il.co.radioapp.databinding.ItemSearchStationBinding
import il.co.radioapp.model.Station
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.util.AssetImageUtil

class SearchStationSheet : BottomSheetDialogFragment() {

    private var _b: FragmentSearchStationBinding? = null
    private val b get() = _b!!
    var onStationSelected: ((Station) -> Unit)? = null

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSearchStationBinding.inflate(inf, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        val allStations = CatalogRepository.getAllStations().sortedBy { it.name }
        val adapter = StationPickAdapter { station -> dismiss(); onStationSelected?.invoke(station) }
        b.rvSearchStations.adapter = adapter
        adapter.submitList(allStations)

        b.svStation.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(q: String?): Boolean {
                val filtered = if (q.isNullOrBlank()) allStations
                               else allStations.filter { it.name.contains(q, ignoreCase = true) }
                adapter.submitList(filtered); return true
            }
            override fun onQueryTextSubmit(q: String?) = false
        })
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    private inner class StationPickAdapter(val onPick: (Station) -> Unit) :
        ListAdapter<Station, StationPickAdapter.VH>(object : DiffUtil.ItemCallback<Station>() {
            override fun areItemsTheSame(a: Station, b: Station) = a.id == b.id
            override fun areContentsTheSame(a: Station, b: Station) = a == b
        }) {
        inner class VH(val b: ItemSearchStationBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(s: Station) {
                b.tvSearchName.text = s.name
                AssetImageUtil.loadLogo(b.root.context, s.id, s.logoUrl, b.ivSearchLogo)
                b.root.setOnClickListener { onPick(s) }
            }
        }
        override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
            VH(ItemSearchStationBinding.inflate(LayoutInflater.from(p.context), p, false))
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    }
}
