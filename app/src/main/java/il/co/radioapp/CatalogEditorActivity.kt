package il.co.radioapp

import android.os.Bundle; import android.view.View; import android.widget.EditText; import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog; import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper; import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import il.co.radioapp.adapter.CatalogEditorAdapter; import il.co.radioapp.adapter.CatalogNodeAdapter
import il.co.radioapp.databinding.ActivityCatalogEditorBinding
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.repository.NodeType; import il.co.radioapp.repository.ResolvedCategory; import il.co.radioapp.repository.ResolvedNode
import kotlinx.coroutines.launch

class CatalogEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogEditorBinding
    private var catAdapter:  CatalogEditorAdapter? = null
    private var nodeAdapter: CatalogNodeAdapter?   = null
    private var currentCategoryId:   String? = null
    private var currentCategoryName: String? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        binding = ActivityCatalogEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        lifecycleScope.launch { CatalogRepository.init(this@CatalogEditorActivity); showCategoryList() }
        binding.fabAdd.setOnClickListener { onFabClick() }
    }

    // ── מסך 1: קטגוריות ─────────────────────────────────────────
    private fun showCategoryList() {
        currentCategoryId   = null
        currentCategoryName = null
        supportActionBar?.title = "ניהול תחנות"
        binding.layoutInnerHeader.visibility = View.GONE

        val cats = CatalogRepository.resolveCategories().toMutableList()
        catAdapter = CatalogEditorAdapter(
            items     = cats,
            onEdit    = { cat -> showRenameDialog(cat.displayName) { n ->
                CatalogRepository.renameCategory(this, cat.id, n); showCategoryList() } },
            onDelete  = { cat, pos -> confirmCatDelete(cat, pos) },
            onDrillIn = { cat -> showCategoryItems(cat) }
        )
        binding.rvEditor.adapter = catAdapter
        attachCatDragHelper()
    }

    // ── מסך 2: פריטים בתוך קטגוריה ──────────────────────────────
    private fun showCategoryItems(cat: ResolvedCategory) {
        currentCategoryId   = cat.id
        currentCategoryName = cat.displayName
        supportActionBar?.title = cat.displayName
        binding.layoutInnerHeader.visibility = View.VISIBLE
        binding.tvInnerTitle.text = "לחיצה ארוכה על 🗑 = הסתרה | גרור = שינוי סדר"

        val nodes = CatalogRepository.resolveCategoryItems(cat.id).toMutableList()
        nodeAdapter = CatalogNodeAdapter(
            items         = nodes,
            onEdit        = { node -> handleNodeEdit(node) },
            onToggleHide  = { node, pos -> handleNodeHide(node, pos) },
            onDelete      = { node, pos -> handleNodeDelete(node, pos) }
        )
        binding.rvEditor.adapter = nodeAdapter
        attachNodeDragHelper()
    }

    // ── עריכת פריט ───────────────────────────────────────────────
    private fun handleNodeEdit(node: ResolvedNode) {
        val catId = currentCategoryId ?: return
        when (node.type) {
            NodeType.STATION -> {
                val sheet = StationEditorBottomSheet.newInstance(node.id, catId)
                sheet.onSaved = { showCategoryItems(ResolvedCategory(catId, currentCategoryName ?: catId)) }
                sheet.show(supportFragmentManager, "station_editor")
            }
            NodeType.SUBCATEGORY, NodeType.SEPARATOR -> {
                showRenameDialog(node.displayName) { newName ->
                    CatalogRepository.renameCategoryItem(this, catId, node.id, newName)
                    showCategoryItems(ResolvedCategory(catId, currentCategoryName ?: catId))
                }
            }
        }
    }

    // ── הסתרה / הצגה ─────────────────────────────────────────────
    private fun handleNodeHide(node: ResolvedNode, pos: Int) {
        val catId = currentCategoryId ?: return
        if (node.isHidden) {
            CatalogRepository.restoreCategoryItem(this, catId, node.id)
            nodeAdapter?.updateItem(pos, node.copy(isHidden = false))
        } else {
            CatalogRepository.hideCategoryItem(this, catId, node.id)
            nodeAdapter?.updateItem(pos, node.copy(isHidden = true))
        }
    }

    // ── מחיקת פריט + Undo ────────────────────────────────────────
    private fun handleNodeDelete(node: ResolvedNode, pos: Int) {
        val catId = currentCategoryId ?: return
        val saved = nodeAdapter?.removeItem(pos) ?: return
        CatalogRepository.deleteCategoryItem(this, catId, node.id)
        Snackbar.make(binding.root, "${node.displayName} הוסר", Snackbar.LENGTH_LONG)
            .setAction("ביטול") {
                CatalogRepository.addStationToCategory(this, catId, node.id)
                nodeAdapter?.restoreItem(saved, pos)
            }.show()
        nodeAdapter?.getOrderedIds()?.let { CatalogRepository.reorderCategoryItems(this, catId, it) }
    }

    // ── FAB ──────────────────────────────────────────────────────
    private fun onFabClick() {
        val catId = currentCategoryId
        if (catId == null) {
            showInputDialog("קטגוריה חדשה", "שם הקטגוריה", "") { name ->
                CatalogRepository.addCategory(this, name); showCategoryList() }
        } else {
            val sheet = AddCategoryItemSheet.newInstance()
            sheet.onExistingStation = {
                val search = SearchStationSheet()
                search.onStationSelected = { station ->
                    CatalogRepository.addStationToCategory(this, catId, station.id)
                    showCategoryItems(ResolvedCategory(catId, currentCategoryName ?: catId))
                }
                search.show(supportFragmentManager, "search_station")
            }
            sheet.onNewStation = {
                val editor = StationEditorBottomSheet.newInstance("", catId)
                editor.onSaved = { showCategoryItems(ResolvedCategory(catId, currentCategoryName ?: catId)) }
                editor.show(supportFragmentManager, "new_station")
            }
            sheet.onSubcategory = { name ->
                CatalogRepository.addSubcategory(this, catId, name)
                showCategoryItems(ResolvedCategory(catId, currentCategoryName ?: catId))
            }
            sheet.onSeparator = { text ->
                CatalogRepository.addSeparator(this, catId, text)
                showCategoryItems(ResolvedCategory(catId, currentCategoryName ?: catId))
            }
            sheet.show(supportFragmentManager, "add_item")
        }
    }

    // ── מחיקת קטגוריה + Undo ─────────────────────────────────────
    private fun confirmCatDelete(cat: ResolvedCategory, pos: Int) {
        val saved = catAdapter?.removeItem(pos) ?: return
        CatalogRepository.deleteCategory(this, cat.id)
        Snackbar.make(binding.root, "${cat.displayName} נמחקה", Snackbar.LENGTH_LONG)
            .setAction("ביטול") {
                CatalogRepository.restoreCategory(this, cat.id)
                catAdapter?.restoreItem(saved, pos)
            }.show()
        catAdapter?.getOrderedIds()?.let { CatalogRepository.reorderCategories(this, it) }
    }

    // ── Back ──────────────────────────────────────────────────────
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentCategoryId != null) showCategoryList() else super.onBackPressed()
    }

    // ── dialogs ───────────────────────────────────────────────────
    private fun showRenameDialog(current: String, onConfirm: (String) -> Unit) =
        showInputDialog("שינוי שם", "שם חדש", current, onConfirm)

    private fun showInputDialog(title: String, hint: String, default: String, onConfirm: (String) -> Unit) {
        val et = EditText(this).apply {
            setText(default); selectAll()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { leftMargin = 48; rightMargin = 48 }
        }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("אישור") { _, _ -> et.text.toString().trim().let { if (it.isNotEmpty()) onConfirm(it) } }
            .setNegativeButton("ביטול", null).show()
    }

    // ── drag helpers ──────────────────────────────────────────────
    private fun attachCatDragHelper() {
        val cb = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                catAdapter?.moveItem(v.bindingAdapterPosition, t.bindingAdapterPosition); return true }
            override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}
            override fun clearView(rv: RecyclerView, v: RecyclerView.ViewHolder) {
                super.clearView(rv, v)
                catAdapter?.getOrderedIds()?.let { CatalogRepository.reorderCategories(this@CatalogEditorActivity, it) }
            }
        }
        val ith = ItemTouchHelper(cb); ith.attachToRecyclerView(binding.rvEditor); catAdapter?.touchHelper = ith
    }

    private fun attachNodeDragHelper() {
        val cb = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                nodeAdapter?.moveItem(v.bindingAdapterPosition, t.bindingAdapterPosition); return true }
            override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}
            override fun clearView(rv: RecyclerView, v: RecyclerView.ViewHolder) {
                super.clearView(rv, v)
                val catId = currentCategoryId ?: return
                nodeAdapter?.getOrderedIds()?.let { CatalogRepository.reorderCategoryItems(this@CatalogEditorActivity, catId, it) }
            }
        }
        val ith = ItemTouchHelper(cb); ith.attachToRecyclerView(binding.rvEditor); nodeAdapter?.touchHelper = ith
    }
}
