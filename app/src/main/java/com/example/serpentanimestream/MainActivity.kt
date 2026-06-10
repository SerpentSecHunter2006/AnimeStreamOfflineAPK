@file:Suppress("SpellCheckingInspection", "unused", "RedundantQualifierName", "JavascriptInterface")

package com.example.serpentanimestream
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import com.example.serpentanimestream.ui.theme.SerpentAnimeStreamTheme
import java.io.ByteArrayInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
@SuppressLint("NewApi", "MissingPermission", "SetJavaScriptEnabled", "ObsoleteSdkInt")
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var currentTab by mutableStateOf("home")
    private var isOffline by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isOffline = !isNetworkAvailable()

        setContent {
            SerpentAnimeStreamTheme {
                if (isOffline) {
                    OfflineScreen { isOffline = !isNetworkAvailable() }
                } else {
                    MainScreen()
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0E0E13).copy(alpha = 0.98f),
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        icon = { Text("🏠", fontSize = 20.sp) },
                        label = { Text("Beranda", fontSize = 11.sp) },
                        selected = currentTab == "home" || currentTab == "detail",
                        onClick = {
                            currentTab = "home"
                            webView?.evaluateJavascript("switchTab('home')", null)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF2D4A),
                            selectedTextColor = Color(0xFFFF2D4A),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        icon = { Text("🔍", fontSize = 20.sp) },
                        label = { Text("Cari", fontSize = 11.sp) },
                        selected = currentTab == "search",
                        onClick = {
                            currentTab = "search"
                            webView?.evaluateJavascript("switchTab('search')", null)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF2D4A),
                            selectedTextColor = Color(0xFFFF2D4A),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        icon = { Text("🕐", fontSize = 20.sp) },
                        label = { Text("Riwayat", fontSize = 11.sp) },
                        selected = currentTab == "history",
                        onClick = {
                            currentTab = "history"
                            webView?.evaluateJavascript("switchTab('history')", null)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF2D4A),
                            selectedTextColor = Color(0xFFFF2D4A),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        icon = { Text("⚙️", fontSize = 20.sp) },
                        label = { Text("Pengaturan", fontSize = 11.sp) },
                        selected = currentTab == "settings",
                        onClick = { currentTab = "settings" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF2D4A),
                            selectedTextColor = Color(0xFFFF2D4A),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0E0E13))
            ) {
                // Layer 1: WebView (Tampilan Utama Anime)
                SerpentWebView(
                    activity = this@MainActivity,
                    onWebViewCreated = { webView = it },
                    onPageChanged = { page -> 
                        if (page != "detail") {
                            currentTab = page
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 2: Settings (Native Compose) menutupi WebView jika aktif
                if (currentTab == "settings") {
                    SettingsScreen(
                        onClearCache = {
                            // Hapus cache natif
                            getSharedPreferences("SerpentPrefs", MODE_PRIVATE).edit { clear() }
                            // Hapus history di JS
                            webView?.evaluateJavascript("History.clear(); renderHistory();", null)
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Berhasil")
                                .setMessage("Cache & Riwayat telah dibersihkan.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    )
                }
            }

            // Tangani tombol back natif
            BackHandler {
                if (currentTab == "settings") {
                    currentTab = "home"
                    webView?.evaluateJavascript("switchTab('home')", null)
                } else {
                    webView?.let { wv ->
                        wv.evaluateJavascript("window.goBack()") { result ->
                            if (result == "false" || result == null) {
                                showExitDialog()
                            }
                        }
                    } ?: showExitDialog()
                }
            }
        }
    }

    @Composable
    fun OfflineScreen(onRetry: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0E13))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "⚠️",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tidak Ada Koneksi Internet",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Aplikasi butuh internet untuk memuat gambar dan video. Mohon aktifkan data seluler atau Wi-Fi Anda.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D4A))
            ) {
                Text("↻ Coba Lagi", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun SettingsScreen(onClearCache: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0E13))
                .padding(16.dp)
        ) {
            Text(
                "⚙️ Pengaturan",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingsItem(icon = "🗑️", text = "Bersihkan Cache & Riwayat", onClick = {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Konfirmasi")
                            .setMessage("Apakah Anda yakin ingin membersihkan riwayat tontonan?")
                            .setPositiveButton("Ya, Bersihkan") { _, _ -> onClearCache() }
                            .setNegativeButton("Batal", null)
                            .show()
                    })
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2A2A36)))
                    SettingsItem(icon = "ℹ️", text = "Versi Aplikasi", trailing = "v1.0.0")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2A2A36)))
                    SettingsItem(icon = "🛡️", text = "Kebijakan Privasi")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "SerpentSec Hunter Logo",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("SERPENTSEC HUNTER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                Text("Aplikasi Streaming Anime Luring Cepat dan Aman.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    @Composable
    fun SettingsItem(icon: String, text: String, trailing: String = "", onClick: (() -> Unit)? = null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
            if (trailing.isNotEmpty()) {
                Text(trailing, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Keluar Aplikasi")
            .setMessage("Apakah Anda yakin ingin keluar dari Serpent AnimeStream?")
            .setPositiveButton("Ya, Keluar") { _, _ -> finish() }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }
}

/* ════════════════════════════════════════════
   WEBVIEW COMPOSABLE
════════════════════════════════════════════ */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SerpentWebView(
    activity: MainActivity,
    onWebViewCreated: (WebView) -> Unit,
    onPageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    loadsImagesAutomatically = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mediaPlaybackRequiresUserGesture = false
                    setSupportMultipleWindows(true) // Enable so onCreateWindow is called
                    javaScriptCanOpenWindowsAutomatically = false // Block JS popups
                }
                addJavascriptInterface(AndroidBridge(activity, onPageChanged), "AndroidBridge")
                webViewClient = SerpentWebViewClient(context)
                webChromeClient = object : WebChromeClient() {
                    // Block ALL popup windows (ads)
                    override fun onCreateWindow(
                        view: WebView?, isDialog: Boolean,
                        isUserGesture: Boolean, resultMsg: android.os.Message?
                    ): Boolean = false
                }
                loadUrl("https://serpent.local/index.html")
                onWebViewCreated(this)
            }
        }
    )
}

/* ════════════════════════════════════════════
   CUSTOM WEBVIEW CLIENT
════════════════════════════════════════════ */
class SerpentWebViewClient(private val context: Context) : WebViewClient() {

    private val adHosts = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "popads.net", "propellerads.com", "adsterranetwork.com", "yllix.com",
        "exoclick.com", "hilltopads.com", "monetag.com", "a-ads.com", "mads.com",
        "adform.net", "rubiconproject.com", "openx.net", "pubmatic.com", "adskeeper.co.uk",
        "outbrain.com", "taboola.com", "adbull.com", "shopee.co.id", "lazada.co.id", 
        "tokopedia.com", "shopeemobile.com", "mgid.com", "revcontent.com",
        "admaven.com", "adsterra.com", "bidgear.com", "popcash.net", "onclickads.net",
        "syndication", "exosrv.com", "realsrv.com", "bet365", "1xbet", "sbobet",
        "judi", "togel", "/pop.js", "/ads.js", "/ad.js", "/apu.php", "/popunder",
        "/popcash", "/onclickads", "popup", "adnetwork"
    )

    private fun isAd(url: String): Boolean {
        return adHosts.any { url.contains(it) }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

        if (isAd(url)) {
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        if (url.startsWith("https://serpent.local/")) {
            if (url.endsWith("index.html") || url == "https://serpent.local/") {
                return try {
                    val inputStream = context.assets.open("index.html")
                    WebResourceResponse("text/html", "UTF-8", inputStream)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg")) {
                val fileName = url.substringAfterLast("/")
                return try {
                    val inputStream = context.assets.open(fileName)
                    val mimeType = if (url.endsWith(".png")) "image/png" else "image/jpeg"
                    WebResourceResponse(mimeType, "UTF-8", inputStream)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (url.endsWith("episodes.enc")) {
                return try {
                    val decrypted = decryptEpisodesFile(context)
                    WebResourceResponse(
                        "application/json",
                        "UTF-8",
                        200,
                        "OK",
                        mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Content-Type" to "application/json; charset=utf-8"
                        ),
                        ByteArrayInputStream(decrypted)
                    )
                } catch (e: Exception) {
                    val errorMsg = """{"error":"Decryption failed: ${e.message}"}"""
                    WebResourceResponse(
                        "application/json",
                        "UTF-8",
                        500,
                        "Error",
                        emptyMap(),
                        ByteArrayInputStream(errorMsg.toByteArray())
                    )
                }
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (isAd(url)) return true // Block any URL opening from ad domains
        return !url.startsWith("file://")
    }
}

fun decryptEpisodesFile(context: Context): ByteArray {
    val k = byteArrayOf(
        115, 101, 114, 112, 101, 110, 116, 95,
        115, 101, 99, 95, 104, 117, 110, 116,
        101, 114, 95, 97, 110, 105, 109, 101,
        95, 115, 116, 114, 101, 97, 109, 95
    )
    val v = byteArrayOf(
        49, 50, 51, 52, 53, 54, 55, 56,
        57, 48, 49, 50, 51, 52, 53, 54
    )
    val secretKey = SecretKeySpec(k, "AES")
    val ivSpec = IvParameterSpec(v)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    val encryptedBytes: ByteArray = context.assets.open("episodes.enc").use { it.readBytes() }
    return cipher.doFinal(encryptedBytes)
}

class AndroidBridge(
    private val activity: MainActivity,
    private val onPageChanged: (String) -> Unit
) {
    @JavascriptInterface
    fun onNavigate(page: String) {
        activity.runOnUiThread { onPageChanged(page) }
    }

    @JavascriptInterface
    fun saveData(key: String, value: String) {
        activity.getSharedPreferences("SerpentPrefs", 0).edit {
            putString(key, value)
        }
    }

    @JavascriptInterface
    fun loadData(key: String): String {
        return activity.getSharedPreferences("SerpentPrefs", 0)
            .getString(key, "{}") ?: "{}"
    }
}