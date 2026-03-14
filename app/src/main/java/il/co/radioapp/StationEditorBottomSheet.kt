package il.co.radioapp

import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import il.co.radioapp.databinding.FragmentStationEditorBinding
import il.co.radioapp.repository.CatalogRepository
import il.co.radioapp.repository.UserStation
import kotlinx.coroutines.Dispatchers; import kotlinx.coroutines.launch; import kotlinx.coroutines.withContext
import java.net.HttpURLConnection; import java.net.URL

class StationEditorBottomSheet : BottomSheetDialogFragment() {

    private var _b: FragmentStationEditorBinding? = null
    private val b get() = _b!!

    var onSaved: (() -> Unit)? = null

    companion object {
        private const val ARG_STATION_ID = "sid"
        private const val ARG_CAT_ID     = "cid"

        fun newInstance(stationId: String, catId: String) =
            StationEditorBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_STATION_ID, stationId)
                    putString(ARG_CAT_ID, catId)
                }
            }
    }

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentStationEditorBinding.inflate(inf, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stationId = arguments?.getString(ARG_STATION_ID) ?: ""
        val catId     = arguments?.getString(ARG_CAT_ID) ?: ""
        val isNew     = stationId.isBlank()

        b.tvEditorTitle.text = if (isNew) "תחנה חדשה" else "עריכת תחנה"

        // טען נתונים קיימים
        if (!isNew) {
            val station = CatalogRepository.getStation(stationId)
            b.etStationName.setText(station?.name ?: "")
            b.etStreamUrl.setText(station?.streamUrl ?: "")
            b.etLogoUrl.setText(station?.logoUrl ?: "")
        }

        // בדיקת חיבור
        b.btnTestUrl.setOnClickListener {
            val url = b.etStreamUrl.text.toString().trim()
            if (url.isBlank()) { b.tvTestResult.text = "הזן URL"; return@setOnClickListener }
            b.tvTestResult.text = "⏳ בודק..."
            lifecycleScope.launch(Dispatchers.IO) {
                val result = try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000; conn.readTimeout = 3000
                    conn.requestMethod = "GET"; conn.instanceFollowRedirects = true
                    val code = conn.responseCode; conn.disconnect()
                    if (code in 200..299) "✅ $code OK" else "⚠️ קוד: $code"
                } catch (e: Exception) { "❌ ${e.message?.take(50)}" }
                withContext(Dispatchers.Main) { b.tvTestResult.text = result }
            }
        }

        b.btnCancel.setOnClickListener { dismiss() }

        b.btnSave.setOnClickListener {
            val name   = b.etStationName.text.toString().trim()
            val url    = b.etStreamUrl.text.toString().trim()
            val logo   = b.etLogoUrl.text.toString().trim().ifBlank { null }
            val ctx    = requireContext()

            if (isNew) {
                if (name.isBlank() || url.isBlank()) {
                    Toast.makeText(ctx, "שם ו-URL חובה", Toast.LENGTH_SHORT).show(); return@setOnClickListener
                }
                CatalogRepository.addCustomStation(ctx, catId, name, url, logo)
            } else {
                val existing = CatalogRepository.getStation(stationId)
                CatalogRepository.overrideStation(ctx, stationId, UserStation(
                    id              = stationId,
                    nameOverride    = name.ifBlank { null },
                    streamUrlOverride = url.ifBlank { existing?.streamUrl },
                    logoUrlOverride = logo,
                    isCustom        = stationId.startsWith("custom_")
                ))
            }
            onSaved?.invoke(); dismiss()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
