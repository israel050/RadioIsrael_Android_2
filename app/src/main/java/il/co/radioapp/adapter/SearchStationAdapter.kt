package il.co.radioapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.databinding.ItemStationSearchBinding
import il.co.radioapp.model.Station
import il.co.radioapp.util.AssetImageUtil

class SearchStationAdapter(
    private val onClick: (Station) -> Unit
) : ListAdapter<Station, SearchStationAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Station>() {
            override fun areItemsTheSame(a: Station, b: Station) = a.id == b.id
            override fun areContentsTheSame(a: Station, b: Station) = a == b
        }
    }

    inner class VH(val b: ItemStationSearchBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemStationSearchBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = currentList[pos]
        h.b.tvName.text = s.name
        h.b.tvCategory.text = s.categories.firstOrNull() ?: ""
        AssetImageUtil.loadLogo(h.b.root.context, s.id, s.logoUrl, h.b.ivLogo)
        h.b.root.setOnClickListener { onClick(s) }
    }
}



