package il.co.radioapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.databinding.ItemStationAddableBinding
import il.co.radioapp.model.Station
import il.co.radioapp.util.AssetImageUtil

class AddableStationAdapter(
    private val onAdd: (Station) -> Unit
) : ListAdapter<Station, AddableStationAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Station>() {
            override fun areItemsTheSame(a: Station, b: Station) = a.id == b.id
            override fun areContentsTheSame(a: Station, b: Station) = a == b
        }
    }

    inner class VH(val b: ItemStationAddableBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemStationAddableBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = currentList[pos]
        h.b.tvName.text = s.name
        AssetImageUtil.loadLogo(h.b.root.context, s.id, s.logoUrl, h.b.ivLogo)
        h.b.btnAdd.setOnClickListener { onAdd(s) }
        h.b.root.setOnClickListener { onAdd(s) }
    }
}



