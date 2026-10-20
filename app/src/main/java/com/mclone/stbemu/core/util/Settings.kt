package com.mclone.stbemu.core.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.io.File
import java.util.UUID

/**
 * Zentrale Laufzeit- und Benutzereinstellung des Emulators.
 *
 * Rekonstruiert aus der Kette `p000.ccr` (Komponente) -> `p000.ccv`
 * (Einstellungsmodell) -> `p000.ccx` (Portraetyp) / `p000.ccb` (Profiltyp).
 * Die Original-App haelt den Zustand in einer Room-Datenbank
 * (`DBSettingsDao`, `DBProfileDao`) plus SharedPreferences.
 */
class Settings(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stbemu_settings", Context.MODE_PRIVATE)

    // ---- Geraeteidentitaet (wird einmalig erzeugt und bleibt stabil) ------

    val androidId: String =
        runCatching { Settings.Secure.getString(context.contentResolver, "android_id") }.getOrNull()
            ?: UUID.randomUUID().toString()

    /**
     * Seriennummer im MAG-Format: 12 hexadezimale Zeichen.
     * Ohne echte Box gibt es keine Originalnummer, daher deterministisch aus
     * [androidId] abgeleitet - wichtig, weil Portale die SN mit dem
     * Handshake verknuepfen.
     */
    var serialNumber: String
        get() = prefs.getString(KEY_SERIAL, null) ?: buildSerial(androidId).also {
            prefs.edit().putString(KEY_SERIAL, it).apply()
        }
        set(value) = prefs.edit().putString(KEY_SERIAL, value).apply()

    /** MAC im Format "00:1A:79:xx:xx:xx" (Infomir-OUI). */
    var macAddress: String
        get() = prefs.getString(KEY_MAC, null) ?: buildMac(androidId).also {
            prefs.edit().putString(KEY_MAC, it).apply()
        }
        set(value) = prefs.edit().putString(KEY_MAC, value).apply()

    // ---- Aktives Portal / Profil -----------------------------------------

    var profileId: String
        get() = prefs.getString(KEY_PROFILE_ID, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_PROFILE_ID, value).apply()

    var portalServer: String
        get() = prefs.getString(KEY_PORTAL_SERVER, "http://127.0.0.1") ?: "http://127.0.0.1"
        set(value) = prefs.edit().putString(KEY_PORTAL_SERVER, value).apply()

    var stbModelId: String
        get() = prefs.getString(KEY_STB_MODEL, "mag-250") ?: "mag-250"
        set(value) = prefs.edit().putString(KEY_STB_MODEL, value).apply()

    // ---- Player -----------------------------------------------------------

    var playerEngine: String
        get() = prefs.getString(KEY_PLAYER, "ijk") ?: "ijk"
        set(value) = prefs.edit().putString(KEY_PLAYER, value).apply()

    var aspectMode: Int
        get() = prefs.getInt(KEY_ASPECT, 0)
        set(value) = prefs.edit().putInt(KEY_ASPECT, value).apply()

    // ---- Darstellung ------------------------------------------------------

    var fullscreen: Boolean
        get() = prefs.getBoolean(KEY_FULLSCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_FULLSCREEN, value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    var orientation: String
        get() = prefs.getString(KEY_ORIENTATION, "landscape") ?: "landscape"
        set(value) = prefs.edit().putString(KEY_ORIENTATION, value).apply()

    var videoMode: String
        get() = prefs.getString(KEY_VIDEO_MODE, "1080p60") ?: "1080p60"
        set(value) = prefs.edit().putString(KEY_VIDEO_MODE, value).apply()

    // ---- Audio / Untertitel ----------------------------------------------

    var audioPrimary: String
        get() = prefs.getString(KEY_AUDIO_PRIMARY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUDIO_PRIMARY, value).apply()

    var audioSecondary: String
        get() = prefs.getString(KEY_AUDIO_SECONDARY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUDIO_SECONDARY, value).apply()

    var subtitlePrimary: String
        get() = prefs.getString(KEY_SUB_PRIMARY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUB_PRIMARY, value).apply()

    var subtitleSecondary: String
        get() = prefs.getString(KEY_SUB_SECONDARY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SUB_SECONDARY, value).apply()

    // ---- Portal-Verhalten -------------------------------------------------

    /** Anhaengen von "; Emulator/1.1.9" an den User-Agent abschaltbar. */
    var emulatorDetection: Boolean
        get() = prefs.getBoolean(KEY_EMU_DETECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_EMU_DETECTION, value).apply()

    /** Portal-Javascript darf den WebView-Zugriff auf das Dateisystem nutzen. */
    var allowFileAccess: Boolean
        get() = prefs.getBoolean(KEY_FILE_ACCESS, true)
        set(value) = prefs.edit().putBoolean(KEY_FILE_ACCESS, value).apply()

    /** Bridge `MyXMLHttpRequestHelper` bereitstellen (AJAX-XML-Fix). */
    var useXmlHttpRequestHelper: Boolean
        get() = prefs.getBoolean(KEY_XHR_HELPER, true)
        set(value) = prefs.edit().putBoolean(KEY_XHR_HELPER, value).apply()

    var fixAjax: Boolean
        get() = prefs.getBoolean(KEY_FIX_AJAX, true)
        set(value) = prefs.edit().putBoolean(KEY_FIX_AJAX, value).apply()

    var firstStartDone: Boolean
        get() = prefs.getBoolean(KEY_FIRST_START, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_START, value).apply()

    // ---- Netzwerk ---------------------------------------------------------

    var proxyHost: String?
        get() = prefs.getString(KEY_PROXY_HOST, null)
        set(value) = prefs.edit().putString(KEY_PROXY_HOST, value).apply()

    var proxyPort: Int
        get() = prefs.getInt(KEY_PROXY_PORT, 0)
        set(value) = prefs.edit().putInt(KEY_PROXY_PORT, value).apply()

    var proxyExclude: String?
        get() = prefs.getString(KEY_PROXY_EXCLUDE, null)
        set(value) = prefs.edit().putString(KEY_PROXY_EXCLUDE, value).apply()

    // ---- Sonstiges --------------------------------------------------------

    var audioOperationalMode: Int by MutableSetting(KEY_AUDIO_OP_MODE, 0)
    var hdmiAudioOut: Int by MutableSetting(KEY_HDMI_AUDIO, 0)
    var drcEnabled: Boolean by MutableSetting(KEY_DRC_ON, false)
    var drcLevel: Int by MutableSetting(KEY_DRC_LEVEL, 0)
    var stereoMode: Int by MutableSetting(KEY_STEREO, 0)
    var micVolume: Int by MutableSetting(KEY_MIC_VOL, 100)
    var inputLanguage: String by MutableSetting(KEY_INPUT_LANG, "en")
    var uiLanguage: String by MutableSetting(KEY_UI_LANG, "")
    var syslogLevel: Int by MutableSetting(KEY_SYSLOG, 0)
    var teletextCharset: String by MutableSetting(KEY_TTX_CHARSET, "UTF-8")
    var screenSaverTimeoutSec: Int by MutableSetting(KEY_SCREENSAVER, 600)

    /** Stellt [MutableSetting]-Properties bereit (schreibt in [prefs]). */
    inner class MutableSetting(private val key: String, private val default: Any) {
        operator fun getValue(thisRef: Any?, property: Any?): Any = readDefault(key, default)
        operator fun setValue(thisRef: Any?, property: Any?, value: Any) = write(key, value)
    }

    private fun readDefault(key: String, default: Any): Any = when (default) {
        is Boolean -> prefs.getBoolean(key, default)
        is Int -> prefs.getInt(key, default)
        is String -> prefs.getString(key, default) ?: default
        else -> default
    }

    private fun write(key: String, value: Any) {
        prefs.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is String -> putString(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
            }
        }.apply()
    }

    // ---- Portal-seitige Geraetewerte --------------------------------------

    /** `env`-Objekt, das das Portal ueber `stb.GetEnv(name)` liest. */
    fun environmentValue(name: String): String? = when (name) {
        "MODEL" -> firmwareModelName()
        "SERIAL" -> serialNumber
        "MAC" -> macAddress
        "DEVICE_ID" -> androidId
        "FW_VER" -> firmwareVersion
        "HW_VER" -> firmwareRevision
        "IMAGE_VERSION" -> firmwareVersion
        "IMAGE_DATE" -> firmwareDate
        "IMAGE_DESC" -> imageDescription
        "V_OUT" -> "rca"
        "VOLUME" -> "100"
        "ITV_CH" -> "1"
        "SCREEN_RES" -> "1280x720"
        "VIDEO_MODE" -> videoMode
        "LANG" -> uiLanguage.ifEmpty { "en" }
        else -> null
    }

    fun setEnvironmentValue(name: String): Boolean {
        if (name !in ENV_WRITABLE) return false
        val value = environmentValue(name) ?: return false
        write(name.lowercase(), value)
        return true
    }

    fun setPlayerOption(key: String, value: String) {
        write("player.$key", value)
    }

    /** Export der Konfiguration im MAG-CFG-Format fuer `stb.ReadCFG()`. */
    fun exportAsPortalConfig(): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        append("<cfg>\n")
        for (name in ENV_EXPORT_ORDER) {
            environmentValue(name)?.let { append("  <$name>${xmlEscape(it)}</$name>\n") }
        }
        append("</cfg>\n")
    }

    fun importFromPortalConfig(xml: String) {
        val doc = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
        runCatching {
            doc.setInput(xml.reader())
            var event = doc.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    val name = doc.name
                    if (name in ENV_WRITABLE) {
                        val value = doc.nextText()
                        write(name.lowercase(), value)
                    }
                }
                event = doc.next()
            }
        }
    }

    fun systemPaths(): String = buildString {
        append("{")
        append("\"tmp\":\"/tmp\",")
        append("\"storage\":\"/storage\",\"var\":\"/var\",")
        append("\"userdata\":\"/home/userdata\"")
        append("}")
    }

    // ---- Von der Firmware-Klasse befuellt --------------------------------

    var firmwareModelName: () -> String = { "MAG 250" }
    var firmwareVersion: () -> String = { "0.2.16-234" }
    var firmwareRevision: () -> String = { "216" }
    var firmwareDate: () -> String = { "Fri Jan 15 15:20:44 EET 2016" }
    var imageDescription: String = "Image: MAG250"
    var videoClock: String = ""

    // ---- Import/Export (Einstellungen sichern) ---------------------------

    fun exportToJson(): String {
        val map = prefs.all.entries.associate { it.key to it.value }
        return org.json.JSONObject(map as Map<*, *>).toString(2)
    }

    fun importFromJson(json: String) {
        runCatching {
            val obj = org.json.JSONObject(json)
            val editor = prefs.edit()
            obj.keys().forEach { key ->
                when (val v = obj.get(key)) {
                    is Boolean -> editor.putBoolean(key, v)
                    is Int -> editor.putInt(key, v)
                    is Long -> editor.putLong(key, v)
                    is Double -> editor.putFloat(key, v.toFloat())
                    is String -> editor.putString(key, v)
                }
            }
            editor.apply()
        }
    }

    /** Writable external cache, in dem PVR/Timeshift abgelegt wird. */
    fun workDirectory(): File = File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        "stbemu",
    ).apply { mkdirs() }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun buildSerial(seed: String): String =
        seed.uppercase().replace(Regex("[^0-9A-F]"), "").padEnd(12, '0').take(12)

    private fun buildMac(seed: String): String {
        val hex = seed.uppercase().replace(Regex("[^0-9A-F]"), "").padEnd(6, '0').take(6)
        return "00:1A:79:${hex.substring(0, 2)}:${hex.substring(2, 4)}:${hex.substring(4, 6)}"
    }

    companion object {
        const val DEBUG = true

        private const val KEY_SERIAL = "device.serial"
        private const val KEY_MAC = "device.mac"
        private const val KEY_PROFILE_ID = "profile.id"
        private const val KEY_PORTAL_SERVER = "portal.server"
        private const val KEY_STB_MODEL = "stb.model"
        private const val KEY_PLAYER = "player.engine"
        private const val KEY_ASPECT = "player.aspect"
        private const val KEY_FULLSCREEN = "ui.fullscreen"
        private const val KEY_KEEP_SCREEN_ON = "ui.keepScreenOn"
        private const val KEY_ORIENTATION = "ui.orientation"
        private const val KEY_VIDEO_MODE = "ui.videoMode"
        private const val KEY_AUDIO_PRIMARY = "audio.primary"
        private const val KEY_AUDIO_SECONDARY = "audio.secondary"
        private const val KEY_SUB_PRIMARY = "sub.primary"
        private const val KEY_SUB_SECONDARY = "sub.secondary"
        private const val KEY_EMU_DETECTION = "emu.detection"
        private const val KEY_FILE_ACCESS = "web.fileAccess"
        private const val KEY_XHR_HELPER = "web.xhrHelper"
        private const val KEY_FIX_AJAX = "portal.fixAjax"
        private const val KEY_FIRST_START = "app.firstStartDone"
        private const val KEY_PROXY_HOST = "net.proxyHost"
        private const val KEY_PROXY_PORT = "net.proxyPort"
        private const val KEY_PROXY_EXCLUDE = "net.proxyExclude"
        private const val KEY_AUDIO_OP_MODE = "audio.opMode"
        private const val KEY_HDMI_AUDIO = "audio.hdmiOut"
        private const val KEY_DRC_ON = "audio.drcOn"
        private const val KEY_DRC_LEVEL = "audio.drcLevel"
        private const val KEY_STEREO = "audio.stereo"
        private const val KEY_MIC_VOL = "audio.micVolume"
        private const val KEY_INPUT_LANG = "lang.input"
        private const val KEY_UI_LANG = "lang.ui"
        private const val KEY_SYSLOG = "system.syslogLevel"
        private const val KEY_TTX_CHARSET = "system.teletextCharset"
        private const val KEY_SCREENSAVER = "system.screenSaverSec"

        private val ENV_WRITABLE = setOf("MODEL", "SERIAL", "MAC", "V_OUT", "VOLUME")

        private val ENV_EXPORT_ORDER = listOf(
            "MODEL", "SERIAL", "MAC", "DEVICE_ID", "FW_VER", "HW_VER",
            "IMAGE_VERSION", "IMAGE_DATE", "IMAGE_DESC",
            "V_OUT", "VOLUME", "ITV_CH", "SCREEN_RES", "VIDEO_MODE", "LANG",
        )
    }
}
