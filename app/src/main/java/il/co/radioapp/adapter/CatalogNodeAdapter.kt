package il.co.radioapp.adapter

import android.view.LayoutInflater; import android.view.MotionEvent; import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper; import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.R
import il.co.radioapp.databinding.ItemCatalogNodeBinding
import il.co.radioapp.repository.NodeType; import il.co.radioapp.repository.ResolvedNode

class CatalogNodeAdapter(
    private val items: MutableList<ResolvedNode>,
    private val onEdit:        (ResolvedNode) -> Unit,
    private val onToggleHide:  (ResolvedNode, Int) -> Unit,
    private val onDelete:      (ResolvedNode, Int) -> Unit
) : RecyclerView.Adapter<CatalogNodeAdapter.VH>() {

    var touchHelper: ItemTouchHelper? = null

    inner class VH(val b: ItemCatalogNodeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(node: ResolvedNode) {
            b.tvNodeName.text = node.displayName
            b.root.alpha  = if (node.isHidden) 0.38f else 1.0f
            b.ivNodeIcon.setImageResource(when (node.type) {
                NodeType.STATION     -> R.drawable.ic_radio
                NodeType.SUBCATEGORY -> R.drawable.ic_category
                NodeType.SEPARATOR   -> R.drawable.ic_drag_handle
            })
            // ✏️ עריכה
            b.btnNodeEdit.setOnClickListener { onEdit(node) }
            // 🗑 מחיקה — לחיצה ארוכה: הסתרה; לחיצה: מחיקה (רק custom)
            b.btnNodeDelete.setOnClickListener { onDelete(node, bindingAdapterPosition) }
            b.btnNodeDelete.setOnLongClickListener {
                onToggleHide(node, bindingAdapterPosition); true
            }
            b.ivDragHandle.setOnTouchListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) touchHelper?.startDrag(this)
                false
            }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(ItemCatalogNodeBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
    override fun getItemCount() = items.size

    fun moveItem(from: Int, to: Int) { items.add(to, items.removeAt(from)); notifyItemMoved(from, to) }
    fun removeItem(pos: Int): ResolvedNode { val i = items.removeAt(pos); notifyItemRemoved(pos); return i }
    fun restoreItem(item: ResolvedNode, pos: Int) { items.add(pos, item); notifyItemInserted(pos) }
    fun updateItem(pos: Int, newNode: ResolvedNode) { items[pos] = newNode; notifyItemChanged(pos) }
    fun getOrderedIds(): List<String> = items.map { it.id }
}
