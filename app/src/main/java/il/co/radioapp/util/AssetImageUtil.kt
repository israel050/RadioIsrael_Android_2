package il.co.radioapp.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import il.co.radioapp.R
import java.io.File

object AssetImageUtil {

    /**
     * Try to load from assets/logos/{id}.{ext}, fallback to remote URL.
     * Supported extensions checked in order: jpg, jpeg, png, webp
     */
    fun loadLogo(ctx: Context, stationId: String, remoteUrl: String?, into: ImageView) {
        val exts = listOf("jpg", "jpeg", "png", "webp")
        val localPath = exts.firstOrNull { ext ->
            try { ctx.assets.open("logos/$stationId.$ext").close(); true }
            catch (_: Exception) { false }
        }

        val source: Any = if (localPath != null)
            android.net.Uri.parse("file:///android_asset/logos/$stationId.$localPath")
        else
            (remoteUrl ?: "")

        Glide.with(ctx)
            .load(source)
            .placeholder(R.drawable.ic_radio)
            .error(R.drawable.ic_radio)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
            .into(into)
    }

    /** Non-circle variant for the player screen */
    fun loadLogoSquare(ctx: Context, stationId: String, remoteUrl: String?, into: ImageView) {
        val exts = listOf("jpg", "jpeg", "png", "webp")
        val localPath = exts.firstOrNull { ext ->
            try { ctx.assets.open("logos/$stationId.$ext").close(); true }
            catch (_: Exception) { false }
        }

        val source: Any = if (localPath != null)
            android.net.Uri.parse("file:///android_asset/logos/$stationId.$localPath")
        else
            (remoteUrl ?: "")

        Glide.with(ctx)
            .load(source)
            .placeholder(R.drawable.ic_radio)
            .error(R.drawable.ic_radio)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(into)
    }
}



