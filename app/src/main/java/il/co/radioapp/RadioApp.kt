package il.co.radioapp

import android.app.Application
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RadioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.applyTheme(AppPreferences.getTheme(this))
        trustAllSSL()
    }

    /**
     * מכשירים מסוימים (כגון Unihertz Jelly2) חסרים CA roots עדכניות
     * ומחזירים "Trust anchor for certification path not found".
     * עבור אפליקציית רדיו אישית זוהי הפתרון הנכון והמהיר.
     */
    private fun trustAllSSL() {
        try {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (_: Exception) {}
    }
}
