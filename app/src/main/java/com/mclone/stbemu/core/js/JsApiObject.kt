package com.mclone.stbemu.core.js

import android.content.Context
import android.graphics.Point
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import org.json.JSONArray

/**
 * Basisklasse aller JavaScript-Bridge-Objekte.
 *
 * Jede Instanz wird ueber `WebView.addJavascriptInterface(objekt, "jsName")`
 * registriert. Portal-JavaScript ruft die Methoden dann ueber das globale
 * Objekt `jsName` auf, z. B. `stb.Play("http://...")`.
 *
 * Rekonstruiert aus `p000.chv` (Basisklasse) und `p000.chs` (Zugriff auf den
 * Host-Kontext). Die Original-App injiziert jedem Bridge-Objekt per Dagger
 * 14 Komponenten der AppComponent (`p000.cux.mo7235a` -> `p000.chw.m6995a` ...).
 * Der Klon buendelt das in einer konkreten [StbHost]-Instanz.
 */
abstract class JsApiObject(@JvmField protected val host: StbHost) {

    /**
     * Vom Original (`p000.chv.__methods()`) dynamisch erzeugte Liste aller
     * exponierten Methoden. Portale nutzen sie zur Feature-Erkennung.
     */
    @JavascriptInterface
    fun __methods(): String {
        val names = mutableListOf<String>()
        for (m in this::class.java.methods) {
            if (m.isAnnotationPresent(JavascriptInterface::class.java)) names.add(m.name)
        }
        return JSONArray(names).toString()
    }

    /**
     * Fallback fuer Methoden, die der Portalcode aufruft, die diese
     * Firmware-Version aber nicht besitzt. Ohne sie wuerfe der WebView einen
     * JavaScript-Fehler und das Portal bleibt weiss.
     */
    @JavascriptInterface
    fun __noSuchMethod__(name: String?, args: String?): String? {
        host.log.d("__noSuchMethod__($name, $args) - von dieser Firmware nicht unterstuetzt")
        return args
    }

    /** Wie im Original (`cib.QueryInterface`): liefert immer dasselbe Objekt zurueck. */
    @JavascriptInterface
    fun QueryInterface(name: String?): JsApiObject = this
}

/** Fenstergeometrie, aus der `$$__screen`-Objekt berechnet. */
data class ScreenMetrics(val width: Int, val height: Int) {
    companion object {
        fun of(context: Context): ScreenMetrics {
            val metrics = context.resources.displayMetrics
            return ScreenMetrics(metrics.widthPixels, metrics.heightPixels)
        }
    }
}

/**
 * Zentraler Host-Kontext fuer alle Bridge-Objekte.
 *
 * Ersetzt im Klon die Dagger-Injektionskette der Original-App. Enthaelt die
 * vom Portal sichtbaren Dienste (Player, Netzwerk, Portal-Steuerung) sowie
 * Standardimplementierungen fuer Geraete, die ein Android-Phone nicht hat
 * (Sat-Tuner, CAS, NAS).
 */
class StbHost(
    val context: Context,
    val settings: com.mclone.stbemu.core.util.Settings,
    val log: com.mclone.stbemu.core.util.Logger,
    val player: com.mclone.stbemu.core.player.PlayerController,
    val network: com.mclone.stbemu.core.net.StalkerApi,
    val firmware: com.mclone.stbemu.core.stb.FirmwareProfile,
) {

    /** Vom Portal aufgebaute Basis-URL. Wird vom Portal selbst gesetzt. */
    var portalUrl: String = "http://127.0.0.1"

    val cookieManager: CookieManager get() = CookieManager.getInstance()

    /** Key/Value-Speicher des Portal-JS (`stbStorage`). */
    val storage: KeyValueStorage = KeyValueStorage(context)

    val screenMetrics: ScreenMetrics by lazy { ScreenMetrics.of(context) }

    // ---- Vom Portal steuerbare Fenster ------------------------------------

    var windowLoader: ((String?) -> Unit)? = null
    var onCloseWindow: (() -> Unit)? = null
    var onNavigate: ((Int) -> Unit)? = null     // -1 zurueck, +1 vorwaerts
    var onReload: (() -> Unit)? = null
    var onStopLoading: (() -> Unit)? = null
    var onZoom: ((Int) -> Unit)? = null
    var onFullscreen: ((Boolean) -> Unit)? = null
    var onMessage: ((String, String?) -> Unit)? = null
    var onVirtualKey: ((String?, Int) -> Unit)? = null
    var onResize: ((Int, Int) -> Unit)? = null
    var onBringToFront: (() -> Unit)? = null

    var currentUrl: String = ""
    var currentTitle: String = ""
    private var activeWindowId = 0

    fun loadUrl(url: String) {
        currentUrl = url
        windowLoader?.invoke(url)
    }

    fun closeWindow() {
        onCloseWindow?.invoke()
    }

    fun goBack() {
        onNavigate?.invoke(-1)
    }

    fun goForward() {
        onNavigate?.invoke(1)
    }

    fun reload() {
        onReload?.invoke()
    }

    fun stopLoading() {
        onStopLoading?.invoke()
    }

    fun setZoom(factor: Int) {
        onZoom?.invoke(factor)
    }

    fun setFullscreen(enabled: Boolean) {
        onFullscreen?.invoke(enabled)
    }

    fun isFullscreen(): Boolean = settings.fullscreen

    fun webViewId(): Int = activeWindowId

    fun broadcast(text: String, target: String? = null) {
        onMessage?.invoke(text, target)
    }

    fun sendVirtualKey(key: String?, code: Int) {
        onVirtualKey?.invoke(key, code)
    }

    fun resizeScreen(w: Int, h: Int) {
        onResize?.invoke(w, h)
    }

    // ---- Fensterverwaltung (stbWindowMgr) --------------------------------

    private val windows = mutableMapOf<Int, String>()

    fun windowInit(url: String?) {
        val id = ++activeWindowId
        windows[id] = url ?: portalUrl
        loadUrl(windows[id] ?: portalUrl)
    }

    fun windowListJson(): String = JSONArray(windows.values.toList()).toString()

    fun windowInfoJson(id: Int): String = windows[id] ?: ""

    fun windowActiveId(): Int = activeWindowId

    fun windowShow(id: Int): Boolean = windows.containsKey(id)

    fun windowHide(id: Int): Boolean = windows.containsKey(id)

    fun windowClose(id: Int): Boolean = windows.remove(id) != null

    fun windowBringToFront() {
        onBringToFront?.invoke()
    }

    fun openDownloadManager(path: String) {
        loadUrl(path)
    }

    // ---- Dateisystem -------------------------------------------------------

    fun listDirectory(path: String, withFiles: Boolean): String =
        com.mclone.stbemu.core.util.StbFs.listDir(context, path, withFiles)

    fun fileExists(path: String): Boolean = com.mclone.stbemu.core.util.StbFs.exists(context, path)

    fun folderExists(path: String): Boolean = com.mclone.stbemu.core.util.StbFs.isDirectory(context, path)

    fun readTextFile(path: String): String? = com.mclone.stbemu.core.util.StbFs.readText(context, path)

    fun writeTextFile(path: String, content: String) {
        com.mclone.stbemu.core.util.StbFs.writeText(context, path, content)
    }

    fun storageInfo(path: String): String = com.mclone.stbemu.core.util.StbFs.storageInfo(context, path)

    // ---- Apps --------------------------------------------------------------

    fun isInstalled(packageName: String): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)

    fun launchApp(packageName: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_MAIN,
                    android.net.Uri.parse("package:$packageName"),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { log.w("launchApp($packageName) fehlgeschlagen", it) }
    }

    // ---- Platzhalter-Dienste (auf Android nicht vorhanden) ---------------

    val pvr: com.mclone.stbemu.core.pvr.PvrController by lazy {
        com.mclone.stbemu.core.pvr.PvrController(context, log)
    }

    val downloads: com.mclone.stbemu.core.net.DownloadManager by lazy {
        com.mclone.stbemu.core.net.DownloadManager(context, log)
    }

    val updater: com.mclone.stbemu.core.net.Updater by lazy {
        com.mclone.stbemu.core.net.Updater(context, settings, log)
    }

    val upnp: com.mclone.stbemu.core.net.UpnpController by lazy {
        com.mclone.stbemu.core.net.UpnpController(context, log)
    }

    val epg: com.mclone.stbemu.core.net.EpgController by lazy {
        com.mclone.stbemu.core.net.EpgController(network, log)
    }
}

/**
 * Persistent Key/Value-Speicher fuer das Portal-JS-Objekt `stbStorage`.
 *
 * Rekonstruiert aus `p000.cir`; dort liegt die Logik in einer
 * SharedPreferences-Instanz.
 */
class KeyValueStorage(context: Context) {
    private val prefs = context.getSharedPreferences("stb_storage", Context.MODE_PRIVATE)

    fun setItem(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getItem(key: String): String? = prefs.getString(key, null)
    fun removeItem(key: String) = prefs.edit().remove(key).apply()
    fun clear() = prefs.edit().clear().apply()
    fun key(index: Int): String? = prefs.all.keys.sorted().getOrNull(index)
}
