package il.co.radioapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import il.co.radioapp.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        h.b.tvCategory.text = categories[pos]
        h.b.root.setOnClickListener { onClick(categories[pos]) }
    }
}



