package com.mclone.stbemu.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.Uri
import com.mclone.stbemu.core.js.StbHost
import com.mclone.stbemu.core.util.Settings
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Netzwerkschicht: HTTP-Client, Proxy, UID-/Hash-Helfer und Portal-API.
 *
 * Rekonstruiert aus `p000.byo`/`p000.byp` (HTTP-Client, Retrofit-aehnlich),
 * `p000.dps`/`p000.dpv` (Request-Builder) und `p000.cuh` (Client-Fabrik).
 */
class StalkerApi(
    private val context: Context,
    private val settings: Settings,
    private val log: com.mclone.stbemu.core.util.Logger,
) {

    /** Truststore der Original-App (`assets/cacert.pem`) wird hier nicht gebraucht. */
    var trustAllCertificates: Boolean = false

    private val customHeaders = mutableMapOf<String, String>()

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .apply {
                settings.proxyHost?.let { host ->
                    val proxy = okhttp3.OkHttpClient.Builder()
                        .proxy(java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(host, settings.proxyPort)))
                        .build()
                    log.i("Netz", "Proxy aktiv: $host:${settings.proxyPort}")
                }
            }
            .build()
    }

    private var proxy: java.net.Proxy? = null

    fun setProxy(host: String, port: Int, exclude: String?) {
        settings.proxyHost = host
        settings.proxyPort = port
        settings.proxyExclude = exclude
        proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(host, port))
    }

    fun resetProxy() {
        settings.proxyHost = null
        settings.proxyPort = 0
        settings.proxyExclude = null
        proxy = null
    }

    fun hasProxy(): Boolean = settings.proxyHost != null

    fun setCustomHeader(name: String, value: String) {
        customHeaders[name] = value
    }

    // ---- Geraete-UID / Hash ----------------------------------------------

    /**
     * `stb.GetUID(...)`. Mehrere Argumente werden indefinierter Reihenfolge
     * benutzt; das Original bevorzugt das erste gesetzte.
     */
    fun stableUid(salts: List<String>): String {
        val salt = salts.firstOrNull { it.isNotBlank() } ?: ""
        val key = settings.profileId + "|" + settings.serialNumber + "|" + salt
        return hash(key).uppercase()
    }

    fun hash(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Variante 1 der Portalanwendung - sha1-basiert. */
    fun hashVersion1(value: String, salt: String? = null): String =
        MessageDigest.getInstance("SHA-1")
            .digest((value + (salt ?: "")).toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ---- Netzwerkinformationen fuer `stb.GetNetwork*` --------------------

    fun networkGateways(): String = wlanGatewayAddress() ?: ""

    fun nameServers(): String {
        val dns = runCatching {
            context.getSystemService(ConnectivityManager::class.java)?.activeNetwork
        }.getOrNull()
        val linkProps = runCatching {
            (context.getSystemService(ConnectivityManager::class.java)
                ?.getLinkProperties(dns))?.dnsServers
                ?.joinToString(",") { it.hostAddress.orEmpty() }
        }.getOrNull()
        return linkProps ?: "8.8.8.8,8.8.4.4"
    }

    private fun wlanGatewayAddress(): String? = runCatching {
        val dhcp = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val info = dhcp?.dhcpInfo ?: return null
        val bytes = info.gateway
        if (bytes == null || bytes.isEmpty()) return null
        bytes.joinToString(".") { (it.toInt() and 0xff).toString() }
    }.getOrNull()

    // ---- Portal-API -------------------------------------------------------

    /**
     * Stalker-MiniGUI-Endpunkte, die die App selbst aufruft.
     * Das Portal ruft dieselben URLs ueber XHR; `PortalAjaxInterceptor`
     * faengt sie ab und wertet sie aus.
     */
    object Endpoints {
        const val ITV_ALL_CHANNELS = "?type=itv&action=get_all_channels"
        const val ITV_ORDERED_LIST = "?type=itv&action=get_ordered_list"
        const val VOD_ORDERED_LIST = "?type=vod&action=get_ordered_list"
        const val STB_LOG_PLAY = "?type=stb&action=log&real_action=play"
        const val STB_PROFILE = "?type=stb&action=get_profile"
        const val STB_AUTH = "type=stb&action=do_auth&login=%s&password=%s&JsHttpRequest=1-xml&"

        /** Logo-Pfad, den das Portal ohnevollstaendige URL erwartet. */
        const val LOGO_BASE = "/stalker_portal/misc/logos/320/"

        val INTERCEPTED = listOf(
            ITV_ALL_CHANNELS, ITV_ORDERED_LIST, VOD_ORDERED_LIST, STB_LOG_PLAY, STB_PROFILE,
        )
    }

    /**
     * Baut die `do_auth`-URL.
     *
     * Wortlueber `com.mvas.stbemu.activities.StalkerLoginActivity.m7894c`:
     * `"%1$s://%2$s:%3$d%4$s?type=stb&action=do_auth&login=%5$s&password=%6$s&JsHttpRequest=1-xml&"`.
     */
    fun buildAuthUrl(server: String, login: String, password: String): String {
        val uri = runCatching { Uri.parse(server) }.getOrNull()
            ?: return "Cannot parse server's URL"
        return buildString {
            append(uri.scheme ?: "http").append("://")
            append(uri.host ?: "")
            append(":").append(if (uri.port > 0) uri.port else 80)
            append(uri.path ?: "")
            append("?").append(Endpoints.STB_AUTH, login, password)
        }
    }

    /** Antwort von `do_auth`: `{ "js": true, "text": "..." }`. */
    fun doAuth(server: String, login: String, password: String): Boolean = runCatching {
        val request = Request.Builder()
            .url(buildAuthUrl(server, login, password))
            .apply { customHeaders.forEach { (k, v) -> header(k, v) } }
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            log.i("Portal", "do_auth -> ${response.code}")
            // Minimaler JSON-Boom: die Middleware nutzt ein Python-Dict-Literal.
            Regex("\"js\"\\s*:\\s*(true|false)").find(body)?.groupValues?.get(1)?.toBoolean()
                ?: (response.isSuccessful && body.contains("\"text\""))
        }
    }.getOrElse {
        log.w("do_auth fehlgeschlagen", it)
        false
    }

    /** GET mit optionalen Custom-Headern; gibt den Body als String. */
    fun get(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .apply { customHeaders.forEach { (k, v) -> header(k, v) } }
            .get()
            .build()
        client.newCall(request).execute().use { it.body?.string() }
    }.getOrElse {
        log.w("GET $url fehlgeschlagen", it)
        null
    }

    /** Ereignis an das Portal senden (`stb.SendEventToPortal`). */
    fun sendEventToPortal(event: String) {
        val base = settings.portalServer.trimEnd('/')
        get("$base$Endpoints.STB_PROFILE&js=$event")
    }

    /** Datei-Download (Update-Service). */
    fun download(url: String, target: File, onProgress: (Long, Long) -> Unit): File? = runCatching {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val total = response.body?.contentLength() ?: -1L
            var written = 0L
            response.body?.byteStream()?.use { input ->
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        written += read
                        onProgress(written, total)
                    }
                }
            }
            target
        }
    }.getOrNull()

    companion object {
        /** MAC der ersten nicht-loopback-Schnittstelle; Fallback auf Settings. */
        fun firstHardwareMac(): String? =
            runCatching {
                NetworkInterface.getNetworkInterfaces().toList()
                    .asSequence()
                    .map { it.hardwareAddress }
                    .firstOrNull { mac ->
                        mac != null && mac.length > 2 &&
                            !mac.startsWith("00:00:00") && !mac.startsWith("02:00:00")
                    }
            }.getOrNull()
    }
}

/**
 * Faengt die Stalker-XHR-Aufrufe des Portals ab und wertet sie lokal aus.
 *
 * Rekonstruiert aus `p000.cvg` (extends `p000.cvd`). Ohne diesen Mechanismus
 * wuerde der WebView die Antworten einer echten MAG-Box erwarten, die es nicht
 * gibt - das Portal zeige dann eine leere Kanalliste.
 *
 * Ablauf:
 *  1. `shouldInterceptRequest` sieht eine der fuenf bekannten URLs.
 *  2. Die Antwort wird geholt und geparst.
 *  3. Das Ergebnis geht an den Kanal-Manager ([onChannels]), damit die
 *     Kanalliste des Emulators befuellt wird.
 */
class PortalAjaxInterceptor(
    private val api: StalkerApi,
    private val log: com.mclone.stbemu.core.util.Logger,
    private val onChannels: (List<PortalChannel>) -> Unit,
    private val onPlaying: (Int) -> Unit,
    private val onAuthRequired: (String) -> Unit,
) {

    /** Erkennt, ob eine URL ueberhaupt abgefangen werden soll. */
    fun matches(url: String): Boolean =
        StalkerApi.Endpoints.INTERCEPTED.any { url.contains(it) }

    /** Auswertung einer abgefangenen Anfrage. */
    fun handle(url: String, body: String?) {
        when {
            url.contains(StalkerApi.Endpoints.STB_LOG_PLAY) -> {
                // content_id=NNN aus der Aufruf-URL extrahieren.
                val id = Regex("content_id=(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (id == 0) log.d("Content id not found!")
                onPlaying(id)
            }

            url.contains(StalkerApi.Endpoints.STB_PROFILE) -> {
                // { "js": {...}, "text": { "status": <n>, ... } }
                val status = Regex("\"status\"\\s*:\\s*(\\d+)").find(body.orEmpty())
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (status == 2) onAuthRequired(url)   // Login erforderlich
            }

            url.contains(StalkerApi.Endpoints.ITV_ALL_CHANNELS) ||
                url.contains(StalkerApi.Endpoints.ITV_ORDERED_LIST) -> {
                parseChannels(body.orEmpty())?.let(onChannels)
            }

            url.contains(StalkerApi.Endpoints.VOD_ORDERED_LIST) -> {
                parseChannels(body.orEmpty())?.let(onChannels)
            }
        }
    }

    /**
     * Zieht die Kanalliste aus der Stalker-Antwort.
     *
     * Die Middleware liefert `{"js": {"data": [ {...}, ... ], "total_items": N}, "text": ""}`
     * bzw. `{"js": true, "text": {...}}` je nach Endpunkt. Die Kanaldaten
     * enthalten `id`, `name`, `logo`, `number`, `status`.
     */
    private fun parseChannels(body: String): List<PortalChannel>? {
        if (body.isBlank()) return null
        return runCatching {
            val array = JSONObject.extractJsonArray(body) ?: return@runCatching emptyList()
            array.mapNotNull { el ->
                val obj = el as? Map<*, *> ?: return@mapNotNull null
                val id = (obj["id"] as? Number)?.toInt() ?: return@mapNotNull null
                PortalChannel(
                    id = id,
                    name = obj["name"]?.toString().orEmpty(),
                    logo = resolveLogo(obj["logo"]?.toString()),
                    number = (obj["number"] as? Number)?.toInt() ?: 0,
                    status = (obj["status"] as? Number)?.toInt() ?: 0,
                )
            }
        }.getOrElse {
            log.w("Kanalantwort nicht lesbar", it)
            null
        }
    }

    /** Logos ohne absoluten Pfad bekommen den Portal-Pfadprefix. */
    private fun resolveLogo(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return if (raw.startsWith("http") || raw.startsWith("/")) raw
        else StalkerApi.Endpoints.LOGO_BASE + raw
    }

    /** Kanal, wie sie der Emulator uebernimmt. */
    data class PortalChannel(
        val id: Int,
        val name: String,
        val logo: String?,
        val number: Int,
        val status: Int,
    )
}

/** Mini-JSON-Helfer, um "js"-Objekte ohne Jackson-Abhaengigkeit zu lesen. */
private object JSONObject {
    /** Sucht rekursiv das erste Array unter Schluesselnamen und liefert es als Maps. */
    fun extractJsonArray(body: String): List<Any?>? {
        val keyIdx = listOf("\"data\"", "\"js\"").firstNotNullOfOrNull { body.indexOf(it) } ?: return null
        val start = body.indexOf('[', keyIdx)
        if (start < 0) return null
        var depth = 0
        var end = -1
        for (i in start until body.length) {
            when (body[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
        }
        if (end < 0) return null
        return runCatching {
            org.json.JSONArray(body.substring(start, end + 1)).let { arr ->
                (0 until arr.length()).map { arr.get(it) }
            }
        }.getOrNull()
    }
}
