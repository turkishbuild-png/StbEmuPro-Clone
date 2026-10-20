package com.mclone.stbemu.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.mclone.stbemu.R
import com.mclone.stbemu.StbEmuApplication
import com.mclone.stbemu.core.js.JsApiObject
import com.mclone.stbemu.core.js.PortalJsApis
import com.mclone.stbemu.core.js.StbHost
import com.mclone.stbemu.core.js.StbJsApi
import com.mclone.stbemu.core.net.StalkerApi
import com.mclone.stbemu.core.player.PlayerController
import com.mclone.stbemu.core.util.AndroidLogger

/**
 * Haupt-Activity – WebView-Host fuer das Stalker/Ministra-Portal.
 *
 * Rekonstruiert aus `com.mvas.stbemu.activities.MainActivity` (1271 Zeilen).
 * Das Original nutzt einen Dagger-Injektionsgraphen (`p000.cpt`/`p000.cux`);
 * der Klon verwendet den [StbHost] aus der Application.
 */
@SuppressLint("SetJavaScriptEnabled")
class MainActivity : AppCompatActivity() {

    private lateinit var app: StbEmuApplication
    private lateinit var host: StbHost
    private lateinit var webView: WebView
    private lateinit var drawer: DrawerLayout
    private lateinit var playerCtrl: PlayerController
    private lateinit var stalkerApi: StalkerApi

    // OSD / Touch
    private var screenButtonsFragment: ScreenButtonsFragment? = null
    private var touchControlFragment: TouchControlFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app = (application as StbEmuApplication)
        host = app.host
        playerCtrl = app.player
        stalkerApi = app.stalkerApi

        // Theme / Orientation
        setTheme(R.style.Theme_StbEmu_Fullscreen)
        requestedOrientation = when (app.settings.orientation) {
            "landscape" -> Activity.SCREEN_ORIENTATION_LANDSCAPE
            "reverse_landscape" -> Activity.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            else -> Activity.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        // Fullscreen / Immersive
        if (app.settings.fullscreen) {
            val decor = window.decorView
            decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        window.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Views
        webView = findViewById(R.id.webview)
        drawer = findViewById(R.id.drawer_layout)
        screenButtonsFragment = supportFragmentManager.findFragmentById(R.id.screen_buttons_fragment) as? ScreenButtonsFragment
        touchControlFragment = supportFragmentManager.findFragmentById(R.id.touch_control_fragment) as? TouchControlFragment

        // WebView konfigurieren
        setupWebView()

        // Player-Engines registrieren (wie im Original: internal, exo, ijk, mpv)
        playerCtrl.register("internal", "Interner Player") { AndroidPlayerBackend(it) }
        playerCtrl.register("exo", "ExoPlayer") { ExoPlayerBackend(it) }
        playerCtrl.register("ijk", "IJKPlayer") { IjkPlayerBackend(it) }
        playerCtrl.register("mpv", "MPV") { MpvPlayerBackend(it) }

        // Aktives Profil anwenden
        app.setFirmware(app.settings.stbModelId)

        // Portal laden
        val portalUrl = intent.getStringExtra("portal") ?: app.settings.portalServer
        loadPortal(portalUrl)

        // Upnp Service binden (Original bindet AndroidUpnpServiceImpl)
        // startService(Intent(this, AndroidUpnpServiceImpl::class.java))

        // Update-Check (Original: MainActivity.m7877a(true))
        checkUpdates()
    }

    /** WebView + JavaScript-Brücke einrichten. */
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setAppCacheEnabled(app.settings.networkEnableCache)
        settings.appCacheMaxSize = app.settings.networkCacheSize.toLong() * 1024
        settings.allowFileAccess = app.settings.allowFileAccess
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL)

        // User-Agent mit Emulator-Detection
        val ua = app.firmware.userAgent(app.settings.emulatorDetection)
        settings.userAgentString = ua
        AndroidLogger.d("WebView", "UA = $ua")

        // Cookie-Handling
        CookieSyncManager.createInstance(this)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)

        // WebViewClient fuer Intercepts (Portal-Ajax, Stalker-Login, etc.)
        webView.webViewClient = PortalWebViewClient()

        // WebChromeClient fuer JS-Dialoge, Console, etc.
        webView.webChromeClient = PortalWebChromeClient()

        // JavaScript-Objekte registrieren
        registerJsObjects()

        // Host-Callbacks
        host.windowLoader = { url -> url?.let { webView.loadUrl(it) } }
        host.onNavigate = { dir -> if (dir < 0) webView.goBack() else webView.goForward() }
        host.onReload = { webView.reload() }
        host.onStopLoading = { webView.stopLoading() }
        host.onZoom = { factor -> webView.setZoom(factor / 100f) }
        host.onFullscreen = { enabled ->
            app.settings.fullscreen = enabled
            recreate()
        }
    }

    /** Registriert alle JS-Bridge-Objekte im WebView. */
    private fun registerJsObjects() {
        val apiObjects = app.firmware.jsApiClasses.mapValues { (_, cls) ->
            cls.getConstructors()[0].newInstance(host) as JsApiObject
        }
        // Spezielle Objekte, die nicht im Firmware-Profil stehen
        apiObjects["stb"] = StbJsApi(host)
        apiObjects["gSTB"] = StbJsApi(host)
        apiObjects["$$__screen"] = PortalJsApis.ScreenJsApi(host)
        apiObjects["stbWindowMgr"] = PortalJsApis.WindowManagerJsApi(host)
        apiObjects["pvrManager"] = PortalJsApis.PvrJsApi(host)
        apiObjects["stbWebWindow"] = PortalJsApis.WebWindowJsApi(host)
        apiObjects["stbDownloadManager"] = PortalJsApis.DownloadManagerJsApi(host)
        apiObjects["stbUpdate"] = PortalJsApis.UpdateJsApi(host)
        apiObjects["timeShift"] = PortalJsApis.TimeShiftJsApi(host)
        apiObjects["stbStorage"] = PortalJsApis.StorageJsApi(host)
        apiObjects["stbUPnP"] = PortalJsApis.UpnpJsApi(host)
        apiObjects["stbPlayerManager"] = PortalJsApis.PlayerManagerJsApi(host)
        apiObjects["epgManager"] = PortalJsApis.EpgJsApi(host)
        apiObjects["stbPlayer0"] = PortalJsApis.PlayerJsApi(host)
        apiObjects["stbPlayerSurface0"] = PortalJsApis.PlayerSurfaceJsApi(host)
        apiObjects["stbBrowser"] = PortalJsApis.BrowserJsApi(host)
        apiObjects["stbWebBrowserSurface0"] = PortalJsApis.WebSurfaceJsApi(host)
        apiObjects["stbAudioSource0"] = PortalJsApis.AudioSourceJsApi(host)
        apiObjects["stbNfs"] = PortalJsApis.NfsJsApi(host)
        apiObjects["stbDisplayManager"] = PortalJsApis.DisplayManagerJsApi(host)
        if (Build.VERSION.SDK_INT == 19) {
            apiObjects["cookieManager"] = PortalJsApis.CookieManagerJsApi(host)
        }

        apiObjects.forEach { (name, obj) ->
            webView.addJavascriptInterface(obj, name)
            AndroidLogger.d("JS-Bridge", "Registered $name -> ${obj::class.simpleName}")
        }
    }

    /** Laedt das Portal und haengt initiale Parameter an. */
    private fun loadPortal(url: String) {
        var target = url
        if (!target.startsWith("http")) target = "http://$target"
        webView.loadUrl(target)
    }

    /** Prueft auf App-Updates (wie Original in onCreate). */
    private fun checkUpdates() {
        if (app.settings.auto_check_updates) {
            val updater = app.updater
            updater.check(null)
            // UI-Update ueber LiveData/Callback waere schoener
        }
    }

    // ---- WebViewClient: Portal-Ajax-Interceptor -----------------------------

    inner class PortalWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            // Stalker-Login weiterleiten
            if (url.contains("action=do_auth")) {
                val uri = Uri.parse(url)
                val login = uri.getQueryParameter("login") ?: ""
                val pass = uri.getQueryParameter("password") ?: ""
                val server = "$uri.scheme://$uri.host:${uri.port}"
                if (stalkerApi.doAuth(server, login, pass)) {
                    Toast.makeText(this@MainActivity, "Login erfolgreich", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Login fehlgeschlagen", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            // Externe Links im Browser oeffnen
            if (url.startsWith("http") && !url.contains("stalker_portal")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            host.currentUrl = url ?: ""
            // Portal-seitige Initialisierung (JS-seitig)
            view?.evaluateJavascript("typeof stb !== 'undefined' && stb.InitPlayer();", null)
        }

        override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
            super.onReceivedError(view, request, error)
            AndroidLogger.w("WebView", "Error ${error?.errorCode}: ${error?.description}")
        }
    }

    // ---- WebChromeClient: JS-Dialoge, Console, Permission -------------------

    inner class PortalWebChromeClient : android.webkit.WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
            AndroidLogger.d("JS-Console", "[${consoleMessage.lineNumber()}] ${consoleMessage.message()}")
            return true
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
            result?.confirm()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
            result?.confirm()
            return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult?): Boolean {
            result?.confirm(defaultValue ?: "")
            return true
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            // Progress-Indikator
        }
    }

    // ---- Tasten / Fernbedienung --------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Direkte Player-Steuerung fuer Tasten, die das Portal nicht braucht
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { playerCtrl.play(""); true }
            KeyEvent.KEYCODE_MEDIA_STOP -> { playerCtrl.stop(); true }
            KeyEvent.KEYCODE_MEDIA_NEXT -> { playerCtrl.seekTo(playerCtrl.positionSec + 30, playerCtrl.durationSec); true }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { playerCtrl.seekTo(maxOf(0, playerCtrl.positionSec - 10), playerCtrl.durationSec); true }
            KeyEvent.KEYCODE_VOLUME_UP -> { playerCtrl.volume = minOf(100, playerCtrl.volume + 5); true }
            KeyEvent.KEYCODE_VOLUME_DOWN -> { playerCtrl.volume = maxOf(0, playerCtrl.volume - 5); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = super.onKeyUp(keyCode, event)

    // ---- Lebenszyklus ------------------------------------------------------

    override fun onStart() {
        super.onStart()
        // Aktives Profil neu laden falls geaendert
        app.setFirmware(app.settings.stbModelId)
    }

    override fun onResume() {
        super.onResume()
        CookieSyncManager.getInstance().startSync()
        // Intent mit Profil-Wechsel?
        intent.getLongExtra("profile-id", -1).also { pid ->
            if (pid > 0) {
                app.settings.profileId = pid
                app.setFirmware(app.settings.stbModelId)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieSyncManager.getInstance().stopSync()
    }

    override fun onStop() {
        super.onStop()
        playerCtrl.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerCtrl.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Portal neu laden falls URL uebergeben
        intent.getStringExtra("portal")?.let { loadPortal(it) }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        playerCtrl.setPictureInPicture(isInPictureInPictureMode)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && app.settings.fullscreen) {
            val decor = window.decorView
            decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    // ---- Optionen-Menue (ActionProvider im Original) -----------------------

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.menu_aspect_ratio -> {
            playerCtrl.aspectMode = (playerCtrl.aspectMode + 1) % 6
            Toast.makeText(this, "Aspect ${playerCtrl.aspectMode}", Toast.LENGTH_SHORT).show()
            true
        }
        R.id.menu_audio_lang -> {
            Toast.makeText(this, "Audio: ${app.settings.audioPrimary}", Toast.LENGTH_SHORT).show()
            true
        }
        R.id.menu_subtitle_lang -> {
            Toast.makeText(this, "Subtitle: ${app.settings.subtitlePrimary}", Toast.LENGTH_SHORT).show()
            true
        }
        R.id.menu_player -> {
            val engines = playerCtrl.availableEngines()
            val idx = engines.indexOf(app.settings.playerEngine)
            val next = engines[(idx + 1) % engines.size]
            app.settings.playerEngine = next
            playerCtrl.engineId = next
            Toast.makeText(this, "Player: $next", Toast.LENGTH_SHORT).show()
            true
        }
        R.id.menu_profile -> {
            // Profil-Auswahl Dialog
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---- Touch-Overlay -----------------------------------------------------

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Touch-Control-Fragment bekommt erste Chance
        touchControlFragment?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}