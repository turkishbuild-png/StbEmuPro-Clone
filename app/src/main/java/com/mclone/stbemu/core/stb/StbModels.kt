package com.mclone.stbemu.core.stb

import com.mclone.stbemu.core.js.AudioSourceJsApi
import com.mclone.stbemu.core.js.CookieManagerJsApi
import com.mclone.stbemu.core.js.DisplayManagerJsApi
import com.mclone.stbemu.core.js.DownloadManagerJsApi
import com.mclone.stbemu.core.js.EpgJsApi
import com.mclone.stbemu.core.js.NfsJsApi
import com.mclone.stbemu.core.js.PlayerManagerJsApi
import com.mclone.stbemu.core.js.PlayerSurfaceJsApi
import com.mclone.stbemu.core.js.PvrJsApi
import com.mclone.stbemu.core.js.ScreenJsApi
import com.mclone.stbemu.core.js.StbJsApi
import com.mclone.stbemu.core.js.TimeShiftJsApi
import com.mclone.stbemu.core.js.UpnpJsApi
import com.mclone.stbemu.core.js.UpdateJsApi
import com.mclone.stbemu.core.js.WebSurfaceJsApi
import com.mclone.stbemu.core.js.WebWindowJsApi
import com.mclone.stbemu.core.js.WindowManagerJsApi

/**
 * Registry aller emulierten Infomir-MAG-Modelle.
 *
 * Rekonstruiert aus `p000.cpt` (Zeile 153), wo 23 Modelle ueber die
 * Annotation `@Model(type, group, name, id)` in eine Dagger-Multibinding-Map
 * `Map<Model, Factory>` eingetragen werden.
 *
 * Die Reihenfolge hier entspricht der Reihenfolge im Original und bestimmt
 * die Reihenfolge im Geraet-Auswahl-Dialog.
 */
object StbModels {

    /** Gemeinsame JS-API. Jede Firmware erbt sie und ergaenzt ggf. eigene Objekte. */
    internal fun baseApi(): Map<String, Class<*>> = linkedMapOf(
        "stb" to StbJsApi::class.java,
        "gSTB" to StbJsApi::class.java,
        "\$\$__screen" to ScreenJsApi::class.java,
        "stbWindowMgr" to WindowManagerJsApi::class.java,
        "pvrManager" to PvrJsApi::class.java,
        "stbWebWindow" to WebWindowJsApi::class.java,
        "stbDownloadManager" to DownloadManagerJsApi::class.java,
        "stbUpdate" to UpdateJsApi::class.java,
        "timeShift" to TimeShiftJsApi::class.java,
        "stbStorage" to com.mclone.stbemu.core.js.StorageJsApi::class.java,
        "stbUPnP" to UpnpJsApi::class.java,
        "stbPlayerManager" to PlayerManagerJsApi::class.java,
        "epgManager" to EpgJsApi::class.java,
        "stbPlayer0" to com.mclone.stbemu.core.js.PlayerJsApi::class.java,
        "stbPlayerSurface0" to PlayerSurfaceJsApi::class.java,
        "stbBrowser" to com.mclone.stbemu.core.js.BrowserJsApi::class.java,
        "stbWebBrowserSurface0" to WebSurfaceJsApi::class.java,
        "stbAudioSource0" to AudioSourceJsApi::class.java,
        "stbNfs" to NfsJsApi::class.java,
        "stbDisplayManager" to DisplayManagerJsApi::class.java,
    )

    /** Nur auf API 19 (KitKat) gibt es den CookieManager-Bridge. */
    internal fun apiForKitKat() = baseApi() + ("cookieManager" to CookieManagerJsApi::class.java)

    val all: List<MagProfile> by lazy {
        listOf(
            MagProfile("mag", "MAG 200", "MAG 200", "mag-200", "0.2.18-r19-pub-200", "134", "Fri Jan 15 15:20:44 EET 2016", "320x240", cas = false, timeShift = false),
            MagProfile("mag", "MAG 24x", "MAG 245", "mag-245", "0.2.18-r14-pub-245", "166", "Wed Mar 25 12:44:32 EET 2015", "720x576", timeShift = true),
            MagProfile("mag", "MAG 24x", "MAG 245D", "mag-245-d", "0.2.18-r14-pub-245", "166", "Wed Mar 25 12:44:32 EET 2015", "720x576", timeShift = true),
            MagProfile("mag", "MAG 25x", "MAG 250", "mag-250", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1280x720", timeShift = true),
            MagProfile("mag", "MAG 25x", "MAG 254", "mag-254", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1280x720", timeShift = true),
            MagProfile("mag", "MAG 25x", "MAG 254W1", "mag-254w1", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1280x720", timeShift = true),
            MagProfile("mag", "MAG 25x", "MAG 254W2", "mag-254w2", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1280x720", timeShift = true),
            MagProfile("mag", "MAG 255", "MAG 255", "mag-255", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 255", "MAG 255W1", "mag-255w1", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 255", "MAG 255W2", "mag-255w2", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 256", "MAG 256", "mag-256", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 260", "MAG 260", "mag-260", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 27x", "MAG 270", "mag-270", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 27x", "MAG 275", "mag-275", "0.2.16-234", "216", "Fri Jan 15 15:20:44 EET 2016", "1080p", timeShift = true),
            MagProfile("mag", "MAG 322", "MAG 322", "mag-322", "0.2.18-r14-pub-322", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
            MagProfile("mag", "MAG 324", "MAG 324", "mag-324", "0.2.18-r14-pub-324", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
            MagProfile("mag", "MAG 35x", "MAG 351", "mag-351", "0.2.18-r14-pub-351", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
            MagProfile("mag", "MAG 35x", "MAG 352", "mag-352", "0.2.18-r14-pub-352", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
            MagProfile("mag", "AuraHD", "AuraHD", "aura-hd", "0.2.18-r14-pub-ahd", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
            MagProfile("mag", "AuraHD", "AuraHD1", "aura-hd-1", "0.2.18-r14-pub-ahd1", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
            MagProfile("mag", "AuraHD", "AuraHD9", "aura-hd-9", "0.2.18-r14-pub-ahd9", "166", "Wed Mar 25 12:44:32 EET 2015", "1080p", timeShift = true),
        )
    }

    fun byId(id: String): MagProfile = all.firstOrNull { it.model.id == id } ?: all[3]

    fun ids(): List<String> = all.map { it.model.id }
}

/**
 * Konkrete Firmware eines MAG-Modells.
 *
 * Rekonstruiert aus `p000.cjr` (Basis, Aufloesungen/Video-Modi) und den
 * `p000.ck*`/`p000.cl*`/`p000.cm*`/`p000.cn*`-Varianten je Modell.
 */
data class MagProfile(
    val type: String,
    val group: String,
    val name: String,
    val id: String,
    val firmwareVersion: String,
    val firmwareRevision: String,
    val buildDate: String,
    val resolution: String,
    val cas: Boolean = true,
    val timeShift: Boolean = true,
) : FirmwareProfile {

    override val model = StbModel(type, group, name, id)

    override val imageDescription: String = "Image: $name"

    /**
     * User-Agent-Vorlage mit `%EMU_DETECTION%`-Platzhalter.
     * Wortlueber `p000.ckx.mo6963a()` fuer die MAG-250-Serie.
     */
    override val imageVersionHtml: String =
        "Mozilla/5.0 (QtEmbedded; U; Linux; C%EMU_DETECTION%) " +
            "AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: $firmwareRevision Safari/533.3"

    override val screenResolutions: List<String>
        get() = listOf("1920x1080", "1280x720", "720x576", resolution).distinct()

    override val defaultScreenResolution: String = resolution

    override val videoModes: List<String>
        get() = listOf("576i", "576p", "720p", "1080i", "1080p", "480i", "720p60", "1080i60", "1080p60")

    override val defaultVideoMode: String = "1080p60"

    override val supportsCas: Boolean = cas
    override val supportsTimeShift: Boolean = timeShift
    override val supportsPvrPersistence: Boolean = timeShift
    override val maxPlayers: Int = 2

    override val jsApiClasses: Map<String, Class<*>> =
        if (android.os.Build.VERSION.SDK_INT == 19) StbModels.apiForKitKat() else StbModels.baseApi()

    override fun toString(): String = name
}
