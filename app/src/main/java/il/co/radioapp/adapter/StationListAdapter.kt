package il.co.radioapp.adapter

import android.view.LayoutInflater; import android.view.ViewGroup
import androidx.core.content.ContextCompat; import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.R; import il.co.radioapp.databinding.ItemSeparatorBinding
import il.co.radioapp.databinding.ItemStationBinding; import il.co.radioapp.databinding.ItemSubcategoryBinding
import il.co.radioapp.model.ListItem; import il.co.radioapp.model.Station
import il.co.radioapp.util.AssetImageUtil; import il.co.radioapp.viewmodel.PlayerViewModel

class StationListAdapter(
    private val items: List<ListItem>, private val vm: PlayerViewModel,
    private val onStation: (Station, List<Station>) -> Unit,
    private val onSubCategory: ((ListItem.SubCategoryItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object { private const val T_ST=0; private const val T_SEP=1; private const val T_SUB=2 }

    private val flat: List<Station> = buildList { fun add(list: List<ListItem>) { for (i in list) when(i) { is ListItem.StationItem -> add(i.station); is ListItem.SubCategoryItem -> add(i.items); else -> Unit } }; add(items) }

    override fun getItemViewType(p: Int) = when(items[p]) { is ListItem.StationItem -> T_ST; is ListItem.SeparatorItem -> T_SEP; else -> T_SUB }
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when(vt) { T_SEP -> SepVH(ItemSeparatorBinding.inflate(inf,parent,false)); T_SUB -> SubVH(ItemSubcategoryBinding.inflate(inf,parent,false)); else -> StVH(ItemStationBinding.inflate(inf,parent,false)) }
    }
    override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) { when(val i=items[p]) { is ListItem.StationItem -> (h as StVH).bind(i.station); is ListItem.SeparatorItem -> (h as SepVH).bind(i.title); is ListItem.SubCategoryItem -> (h as SubVH).bind(i) } }

    inner class StVH(val b: ItemStationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: Station) {
            b.tvName.text = s.name; AssetImageUtil.loadLogo(b.root.context, s.id, s.logoUrl, b.ivLogo)
            val fav = vm.isFavorite(s.id)
            b.btnFav.setImageResource(if(fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            b.btnFav.imageTintList = ContextCompat.getColorStateList(b.root.context, if(fav) R.color.heartActive else R.color.heartInactive)
            b.btnFav.setOnClickListener { vm.toggleFavorite(s.id); notifyItemChanged(bindingAdapterPosition) }
            b.root.isSelected = vm.currentStation.value?.id == s.id
            b.root.setOnClickListener { onStation(s, flat) }
        }
    }
    inner class SepVH(val b: ItemSeparatorBinding) : RecyclerView.ViewHolder(b.root) { fun bind(t: String) { b.tvSeparator.text = t } }
    inner class SubVH(val b: ItemSubcategoryBinding) : RecyclerView.ViewHolder(b.root) { fun bind(sub: ListItem.SubCategoryItem) { b.tvSubcategory.text = sub.name; b.root.setOnClickListener { onSubCategory?.invoke(sub) } } }
}
