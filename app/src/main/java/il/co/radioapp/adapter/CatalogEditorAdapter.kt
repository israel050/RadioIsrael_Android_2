package il.co.radioapp.adapter

import android.view.LayoutInflater; import android.view.MotionEvent; import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper; import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.databinding.ItemCatalogNodeBinding
import il.co.radioapp.repository.ResolvedCategory

class CatalogEditorAdapter(
    private val items: MutableList<ResolvedCategory>,
    private val onEdit:   (ResolvedCategory) -> Unit,
    private val onDelete: (ResolvedCategory, Int) -> Unit,
    private val onDrillIn: (ResolvedCategory) -> Unit
) : RecyclerView.Adapter<CatalogEditorAdapter.VH>() {

    var touchHelper: ItemTouchHelper? = null

    inner class VH(val b: ItemCatalogNodeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(cat: ResolvedCategory) {
            b.tvNodeName.text = cat.displayName
            b.root.setOnClickListener { onDrillIn(cat) }
            b.btnNodeEdit.setOnClickListener { onEdit(cat) }
            b.btnNodeDelete.setOnClickListener { onDelete(cat, bindingAdapterPosition) }
            b.ivDragHandle.setOnTouchListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) touchHelper?.startDrag(this)
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
        VH(ItemCatalogNodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
    override fun getItemCount() = items.size

    fun moveItem(from: Int, to: Int) {
        val item = items.removeAt(from); items.add(to, item)
        notifyItemMoved(from, to)
    }

    fun removeItem(pos: Int): ResolvedCategory {
        val item = items.removeAt(pos); notifyItemRemoved(pos); return item
    }

    fun restoreItem(item: ResolvedCategory, pos: Int) {
        items.add(pos, item); notifyItemInserted(pos)
    }

    fun getOrderedIds(): List<String> = items.map { it.id }
}
