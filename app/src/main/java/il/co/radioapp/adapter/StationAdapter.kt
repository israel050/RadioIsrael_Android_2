package il.co.radioapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.R
import il.co.radioapp.databinding.ItemStationBinding
import il.co.radioapp.model.Station
import il.co.radioapp.util.AssetImageUtil
import il.co.radioapp.viewmodel.PlayerViewModel

class StationAdapter(
    private val stations: List<Station>,
    private val vm: PlayerViewModel,
    private val onClick: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.VH>() {

    inner class VH(val b: ItemStationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemStationBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = stations.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = stations[pos]
        h.b.tvName.text = s.name
        AssetImageUtil.loadLogo(h.b.root.context, s.id, s.logoUrl, h.b.ivLogo)
        bindHeart(h, s.id)
        h.b.btnFav.setOnClickListener {
            vm.toggleFavorite(s.id)
            bindHeart(h, s.id)
        }
        h.b.root.isSelected = vm.currentStation.value?.id == s.id
        h.b.root.setOnClickListener { onClick(s) }
    }

    private fun bindHeart(h: VH, id: String) {
        val fav = vm.isFavorite(id)
        h.b.btnFav.setImageResource(if (fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        h.b.btnFav.imageTintList = ContextCompat.getColorStateList(h.b.root.context,
            if (fav) R.color.heartActive else R.color.heartInactive)
    }
}



