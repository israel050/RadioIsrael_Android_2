package il.co.radioapp.adapter

import android.annotation.SuppressLint
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.databinding.ItemStationSelectedBinding
import il.co.radioapp.model.Station
import il.co.radioapp.util.AssetImageUtil
import java.util.Collections

class SelectedStationAdapter(
    private val onRemove: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<SelectedStationAdapter.VH>() {

    val stations = mutableListOf<Station>()

    inner class VH(val b: ItemStationSelectedBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemStationSelectedBinding.inflate(LayoutInflater.from(p.context), p, false))

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = stations[pos]
        h.b.tvName.text = s.name
        AssetImageUtil.loadLogo(h.b.root.context, s.id, s.logoUrl, h.b.ivLogo)
        h.b.btnRemove.setOnClickListener { onRemove(h.bindingAdapterPosition) }
        h.b.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(h)
            false
        }
    }

    override fun getItemCount() = stations.size

    fun moveItem(from: Int, to: Int) {
        Collections.swap(stations, from, to)
        notifyItemMoved(from, to)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<Station>) {
        stations.clear()
        stations.addAll(list)
        notifyDataSetChanged()
    }
}



