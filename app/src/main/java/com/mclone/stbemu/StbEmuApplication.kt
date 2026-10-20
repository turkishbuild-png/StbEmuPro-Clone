package com.mclone.stbemu

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.mclone.stbemu.core.js.JsApiObject
import com.mclone.stbemu.core.js.PortalJsApis
import com.mclone.stbemu.core.js.StbHost
import com.mclone.stbemu.core.js.StbJsApi
import com.mclone.stbemu.core.net.DownloadManager
import com.mclone.stbemu.core.net.EpgController
import com.mclone.stbemu.core.net.StalkerApi
import com.mclone.stbemu.core.net.Updater
import com.mclone.stbemu.core.net.UpnpController
import com.mclone.stbemu.core.player.PlayerController
import com.mclone.stbemu.core.pvr.PvrController
import com.mclone.stbemu.core.stb.FirmwareProfile
import com.mclone.stbemu.core.stb.MagProfile
import com.mclone.stbemu.core.stb.StbModels
import com.mclone.stbemu.core.util.AndroidLogger
import com.mclone.stbemu.core.util.Settings
import com.mclone.stbemu.core.util.StbFs

/**
 * Application-Klasse – zentraler Aufbau des Abhaengigkeitsgraphen.
 *
 * Ersetzt das Dagger-Setup der Original-App (`p000.cpt`/`p000.cpp`/`p000.cux`).
 * Alle Dienste werden hier als Singletons erzeugt und den Bridge-Objekten
 * ueber den [StbHost] bereitgestellt.
 */
class StbEmuApplication : Application() {

    companion object {
        @Suppress("UNUSED_PARAMETER")
        var instance: StbEmuApplication? = null
            private set

        fun get(context: Context): StbEmuApplication =
            context.applicationContext as StbEmuApplication
    }

    // ---- Kern-Singletons ----------------------------------------------------

    val settings: Settings by lazy { Settings(this) }
    val logger: com.mclone.stbemu.core.util.Logger by lazy { AndroidLogger }
    val player: PlayerController by lazy { PlayerController(this, logger) }
    val stalkerApi: StalkerApi by lazy { StalkerApi(this, settings, logger) }
    val pvr: PvrController by lazy { PvrController(this, logger) }
    val downloads: DownloadManager by lazy { DownloadManager(this, logger) }
    val updater: Updater by lazy { Updater(this, settings, logger) }
    val upnp: UpnpController by lazy { UpnpController(this, logger) }
    val epg: EpgController by lazy { EpgController(stalkerApi, logger) }

    // ---- Firmware-Profil ----------------------------------------------------

    /** Aktives Profil (Startwert = MAG-250). */
    var firmware: FirmwareProfile by lazy { MagProfile.getDefault() }

    /** Aktualisiert das aktive Firmware-Profil (z. B. Profil-Wechsel). */
    fun setFirmware(modelId: String) {
        firmware = StbModels.byId(modelId)
        settings.stbModelId = modelId
        // Player neu initialisieren falls noetig
        player.engineId = firmware.model.id
        logger.i("App", "Firmware gewechselt -> $modelId (${firmware.name})")
    }

    // ---- StbHost fuer JS-Brücke --------------------------------------------

    private val host by lazy { StbHostImpl() }

    inner class StbHostImpl : StbHost {
        override val context: Context = this@StbEmuApplication
        override val settings: Settings = this@StbEmuApplication.settings
        override val log: com.mclone.stbemu.core.util.Logger = logger
        override val player: PlayerController = player
        override val network: StalkerApi = stalkerApi
        override val firmware: FirmwareProfile = this@StbEmuApplication.firmware
        override val portalUrl: String = settings.portalServer

        override fun assetUrl(path: String): String =
            "content://com.mclone.stbemu.asset/$path"

        // ---- Window-Callbacks ------------------------------------------------

        var windowLoader: ((String?) -> Unit)? = null
        var onCloseWindow: (() -> Unit)? = null
        var onNavigate: ((Int) -> Unit)? = null
        var onReload: (() -> Unit)? = null
        var onStopLoading: (() -> Unit)? = null
        var onZoom: ((Int) -> Unit)? = null
        var onFullscreen: ((Boolean) -> Unit)? = null
        var onMessage: ((String, String?) -> Unit)? = null
        var onVirtualKey: ((String?, Int) -> Unit)? = null
        var onResize: ((Int, Int) -> Unit)? = null
        var onBringToFront: (() -> Unit)? = null

        override fun loadUrl(url: String) {
            portalUrl = url
            windowLoader?.invoke(url)
        }

        override fun closeWindow() = onCloseWindow?.invoke()
        override fun goBack() = onNavigate?.invoke(-1)
        override fun goForward() = onNavigate?.invoke(1)
        override fun reload() = onReload?.invoke()
        override fun stopLoading() = onStopLoading?.invoke()
        override fun setZoom(factor: Int) = onZoom?.invoke(factor)
        override fun setFullscreen(enabled: Boolean) = onFullscreen?.invoke(enabled)
        override fun isFullscreen(): Boolean = settings.fullscreen
        override fun webViewId(): Int = 0
        override fun broadcast(text: String, target: String? = null) = onMessage?.invoke(text, target)
        override fun sendVirtualKey(key: String?, code: Int) = onVirtualKey?.invoke(key, code)
        override fun resizeScreen(w: Int, h: Int) = onResize?.invoke(w, h)
        override fun windowBringToFront() = onBringToFront?.invoke()
        override fun currentUrl(): String = portalUrl
        override fun currentTitle(): String = ""
        override fun windowActiveId(): Int = 0
        override fun windowInit(url: String?) = loadUrl(url)
        override fun windowListJson(): String = "[]"
        override fun windowInfoJson(id: Int): String = ""
        override fun windowShow(id: Int): Boolean = false
        override fun windowHide(id: Int): Boolean = false
        override fun windowClose(id: Int): Boolean = false
        override fun openDownloadManager(path: String) = loadUrl(path)

        override fun listDirectory(path: String, withFiles: Boolean): String =
            StbFs.listDir(this, path, withFiles)

        override fun fileExists(path: String): Boolean = StbFs.exists(this, path)
        override fun folderExists(path: String): Boolean = StbFs.isDirectory(this, path)
        override fun readTextFile(path: String): String? = StbFs.readText(this, path)
        override fun writeTextFile(path: String, content: String) = StbFs.writeText(this, path, content)
        override fun storageInfo(path: String): String = StbFs.storageInfo(this, path)

        override fun isInstalled(packageName: String): Boolean =
            runCatching {
                packageManager.getPackageInfo(packageName, 0)
                true
            }.getOrDefault(false)

        override fun launchApp(packageName: String) =
            runCatching {
                startActivity(
                    android.content.Intent(android.content.Intent.ACTION_MAIN)
                        .setData(android.net.Uri.parse("package:$packageName"))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { logger.w("launchApp($packageName)", it) }
    }

    // ---- Lebenszyklus ------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Android-ID loggen (Original: `p000/csg.m8301a` prueft ob "6fe272e7da8789c0")
        val aid = Settings.Secure.getString(contentResolver, "android_id")
        logger.d("App", "android_id = $aid")

        // Timber-Log initialisieren (Ersatz fuer p000.fdc)
        if (Settings.DEBUG) {
            timber.Timber.plant(object : timber.Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    Log.println(priority, tag ?: "StbEmu", message)
                }
            })
        }

        // Work-Verzeichnis sicherstellen
        StbFs.workDir(this).mkdirs()

        // Standard-Firmware in Settings spiegeln (Locale, Player-Default, etc.)
        settings.firmwareModelName = { firmware.model.name }
        settings.firmwareVersion = { firmware.firmwareVersion }
        settings.firmwareRevision = { firmware.firmwareRevision }
        settings.firmwareDate = { firmware.buildDate }
        settings.imageDescription = firmware.imageDescription

        logger.i("App", "StbEmuApplication started (${BuildConfig.VERSION_NAME})")
    }

    override fun onTerminate() {
        player.release()
        super.onTerminate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        settings.m7840a()  // Locale-Update
    }
}

/** Default-Firmware fuer den ersten Start. */
private fun MagProfile.getDefault(): MagProfile = StbModels.byId("mag-250")