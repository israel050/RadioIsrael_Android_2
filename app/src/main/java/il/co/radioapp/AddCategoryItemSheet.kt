package il.co.radioapp

import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup
import android.widget.EditText; import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import il.co.radioapp.databinding.FragmentAddCategoryItemBinding

class AddCategoryItemSheet : BottomSheetDialogFragment() {

    private var _b: FragmentAddCategoryItemBinding? = null
    private val b get() = _b!!

    var onExistingStation: (() -> Unit)?           = null
    var onNewStation: (() -> Unit)?                 = null
    var onSubcategory: ((String) -> Unit)?         = null
    var onSeparator: ((String) -> Unit)?           = null

    companion object {
        fun newInstance() = AddCategoryItemSheet()
    }

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAddCategoryItemBinding.inflate(inf, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.optExistingStation.setOnClickListener { dismiss(); onExistingStation?.invoke() }
        b.optNewStation.setOnClickListener      { dismiss(); onNewStation?.invoke() }
        b.optSubcategory.setOnClickListener     { dismiss(); askName("תת-קטגוריה", "שם התת-קטגוריה") { onSubcategory?.invoke(it) } }
        b.optSeparator.setOnClickListener       { dismiss(); askName("מפריד", "טקסט המפריד") { onSeparator?.invoke(it) } }
    }

    private fun askName(title: String, hint: String, callback: (String) -> Unit) {
        val et = EditText(requireContext()).apply { this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { leftMargin = 48; rightMargin = 48 } }
        AlertDialog.Builder(requireContext())
            .setTitle(title).setView(et)
            .setPositiveButton("אישור") { _, _ ->
                val t = et.text.toString().trim()
                if (t.isNotEmpty()) callback(t) }
            .setNegativeButton("ביטול", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
