package com.mclone.stbemu.core.player

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemeinsame Oberflaeche aller Player-Backends.
 *
 * Das Original hat sechs Backends (siehe `com.mvas.stbemu.core.db.impl.models`:
 * DBInternalPlayerSettingsDao, DBIjkPlayerSettingsDao, DBMpvPlayerSettingsDao,
 * DBExoPlayerSettingsDao, DBExternalPlayerSettingsDao, DBAutoPlayerSettingsDao)
 * und registriert sie ueber die Fabrik `p000.ctc` mit den Schluesseln
 * "internal", "ijk", "mpv", "exo", "external", "auto".
 */
interface PlayerBackend {
    val id: String
    val displayName: String

    /** true, wenn das Backend auf diesem Geraet/SDK-Level nutzbar ist. */
    val available: Boolean

    fun prepare(context: Context)

    /** Konnektiert die Videoausgabe. */
    fun attach(surface: android.view.Surface)

    fun setVideoScalingMode(mode: Int)

    fun play(url: String, headers: Map<String, String> = emptyMap())
    fun stop()
    fun release()

    val isPlaying: Boolean
    val positionMs: Long
    val durationMs: Long
    val bufferedPercent: Int

    fun seekTo(positionMs: Long)

    fun setVolume(volume: Int)
    fun setMuted(muted: Boolean)
    fun setSpeed(percent: Int)

    val videoWidth: Int
    val videoHeight: Int

    /** Spurinformationen aus dem Container. */
    fun mediaInfo(): JSONObject
}

/**
 * Steuert den aktiven Player und uebersetzt die Portal-API
 * (`stb.SetVolume`, `stb.SetPosTime`, ...) auf das Backend.
 *
 * Rekonstruiert aus `p000.crb`/`p000.crc`..`p000.crt` (Player-Manager) und
 * `p000.cbs` (Play-Steuerung), die in `MainActivity` als `f10552t`/`f10555w`
 * injiziert werden.
 */
class PlayerController(
    private val context: Context,
    private val log: com.mclone.stbemu.core.util.Logger,
) {

    private val factories = mutableMapOf<String, (Context) -> PlayerBackend>()
    private val settingsById = mutableMapOf<String, PlayerSettings>()

    var backend: PlayerBackend? = null
        private set

    /** Auswahlmodus: "auto", "internal", "ijk", "mpv", "exo", "external". */
    var engineId: String = "auto"
        set(value) {
            field = value
            selectBackend()
        }

    /** Verfuegbare Engines in der Reihenfolge der automatischen Auswahl. */
    private val fallbackOrder = listOf("exo", "ijk", "mpv", "internal")

    init {
        register("internal", "Interner Player") { AndroidPlayerBackend(it) }
        register("exo", "ExoPlayer") { ExoPlayerBackend(it) }
        register("ijk", "IJKPlayer") { IjkPlayerBackend(it) }
        register("mpv", "MPV") { MpvPlayerBackend(it) }
    }

    fun register(id: String, displayName: String, factory: (Context) -> PlayerBackend) {
        factories[id] = factory
        settingsById[id] = PlayerSettings()
    }

    fun engineIds(): List<String> = factories.keys.sorted()

    fun displayName(id: String): String = when (id) {
        "internal" -> "Interner Player"
        "exo" -> "ExoPlayer"
        "ijk" -> "IJKPlayer"
        "mpv" -> "MPV"
        "external" -> "Externer Player"
        "auto" -> "Automatisch"
        else -> id
    }

    fun availableEngines(): List<String> =
        factories.filterValues { runCatching { it(context).available }.getOrDefault(false) }.keys.sorted()

    fun settings(id: String): PlayerSettings = settingsById.getOrPut(id) { PlayerSettings() }

    private fun selectBackend() {
        val wanted = if (engineId == "auto" || engineId == "external") null else engineId
        val chosen = wanted?.let { factories[it] }
            ?: fallbackOrder.firstNotNullOfOrNull { id ->
                factories[id]?.takeIf { runCatching { it(context).available }.getOrDefault(false) }
            }
        val newBackend = chosen?.invoke(context)
        if (newBackend !== backend) {
            backend?.release()
            backend = newBackend
            newBackend?.prepare(context)
            log.i("Player", "Backend -> ${newBackend?.id}")
        }
    }

    fun attach(context: Context) {
        selectBackend()
        backend?.prepare(context)
    }

    fun attach(surface: android.view.Surface) {
        backend?.attach(surface)
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        log.d("setViewport($x,$y,$width,$height) - vom Player-Backend uebernommen")
    }

    fun setVideoScalingMode(mode: Int) {
        backend?.setVideoScalingMode(mode)
    }

    // ---- Transport --------------------------------------------------------

    fun play(url: String) {
        val target = backend ?: run {
            attach(context)
            backend
        } ?: return
        if (url.isBlank()) return
        target.play(url)
    }

    fun pause() {
        backend?.let { if (it.isPlaying) it.play("") }
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun stop() {
        backend?.stop()
    }

    fun release() {
        backend?.release()
        backend = null
    }

    private var paused = false

    fun isPlaying(): Boolean = !paused && backend?.isPlaying == true

    fun seekTo(positionMs: Long, durationMs: Long) {
        val clamped = positionMs.coerceIn(0, durationMs.coerceAtLeast(0))
        backend?.seekTo(clamped)
    }

    fun setLoop(mode: Int) {
        log.d("setLoop($mode)")
    }

    fun setSpeed(percent: Int) {
        backend?.setSpeed(percent)
    }

    fun setBufferSize(videoKb: Int, audioKb: Int) {
        log.d("setBufferSize($videoKb,$audioKb)")
    }

    fun setPictureInPicture(enabled: Boolean) {
        log.d("setPictureInPicture($enabled)")
    }

    // ---- Eigenschaften ----------------------------------------------------

    var volume: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
            backend?.setVolume(field)
        }

    var muted: Boolean = false
        set(value) {
            field = value
            backend?.setMuted(value)
        }

    var aspectMode: Int = 0
    var alphaLevel: String = "255"
    var syncOffsetMs: Int = 0
    var subtitlesEnabled: Boolean = true
    var subtitleSize: Int = 40
    var subtitleFont: String = ""
    var subtitlePosition: Int = 0
    var subtitleOffset: Int = 0
    var subtitleColor: Int = 0x00FFFFFF
    var subtitleEncoding: String = "UTF-8"
    var subtitleLanguage: String = ""

    val speedPercent: Int get() = 100
    val bufferLoadPercent: Int get() = backend?.bufferedPercent ?: 0
    val pictureInPicture: Boolean = false

    val durationSec: Int get() = ((backend?.durationMs ?: 0) / 1000).toInt()
    val positionSec: Int get() = ((backend?.positionMs ?: 0) / 1000).toInt()

    val positionPercent: Int
        get() {
            val d = backend?.durationMs ?: 0
            if (d <= 0) return 0
            return ((backend?.positionMs ?: 0) * 100 / d).toInt()
        }

    val positionStr: String
        get() {
            val total = backend?.durationMs ?: 0
            fun fmt(ms: Long): String {
                val s = ms / 1000
                return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
            }
            return fmt(backend?.positionMs ?: 0) + "/" + fmt(total)
        }

    // ---- Spur-Auswahl -----------------------------------------------------

    fun selectAudioTrack(pid: Int) {
        log.d("selectAudioTrack($pid)")
    }

    fun selectSubtitleTrack(pid: Int) {
        log.d("selectSubtitleTrack($pid)")
    }

    fun selectAudioLanguage(lang: String) {
        log.d("selectAudioLanguage($lang)")
    }

    fun selectSubtitleLanguage(lang: String) {
        subtitleLanguage = lang
    }

    fun loadExternalSubtitle(path: String) {
        log.d("loadExternalSubtitle($path)")
    }

    fun showSubtitleOverlay(x: Int, y: Int, text: String?) {
        log.d("showSubtitle($x,$y,$text)")
    }

    val audioTrackPid: Int get() = -1
    val subtitleTrackPid: Int get() = -1

    // ---- JSON-Ausgaben fuer `stb.*Info()` --------------------------------

    fun videoInfoJson(): String = JSONObject().apply {
        put("audio", JSONArray().put(1))
        put("video", JSONArray().put(1))
        put("subtitles", if (subtitlesEnabled) JSONArray().put(1) else JSONArray())
        run {
            val w = backend?.videoWidth ?: 0
            val h = backend?.videoHeight ?: 0
            put("VideoWidth", w)
            put("VideoHeight", h)
            put("Audio", 1)
            put("Video", 1)
            put("Subtitles", if (subtitlesEnabled) 1 else 0)
            put("FormatInfo", if (w > 0) "HD" else "SD")
        }
    }.toString()

    fun metadataJson(): String = JSONObject().apply {
        put("service_name", "")
        put("service_id", 0)
        put("logical_channel_number", 0)
        put("virtual_channel_number", 0)
        put("provider", "")
    }.toString()

    fun hlsInfoJson(): String = JSONObject().apply {
        put("MasterPlaylist", "")
        put("AudioTrack", JSONArray())
        put("SubtitleTrack", JSONArray())
    }.toString()

    fun capabilitiesJson(): String = JSONObject().apply {
        put("aspect_conversion", true)
        put("audio_ogg", false)
        put("spatial_navigation", false)
        put("hdr", false)
    }.toString()

    fun statisticsJson(): String = JSONObject().apply {
        put("droppedFrames", 0)
        put("videoWidth", backend?.videoWidth ?: 0)
        put("videoHeight", backend?.videoHeight ?: 0)
    }.toString()

    fun listJson(): String {
        val arr = JSONArray()
        factories.keys.sorted().forEach { id ->
            arr.put(
                JSONObject().apply {
                    put("id", id)
                    put("type", id)
                    put("available", runCatching { factories[id]?.invoke(context)?.available }.getOrDefault(false))
                    put("caption", displayName(id))
                }
            )
        }
        return arr.toString()
    }
}

/** Pro-Engine-Einstellungen (Buffer, Decoder, Hardwarbeschleunigung). */
data class PlayerSettings(
    var videoBufferSizeKb: Int = 1024,
    var audioBufferSizeKb: Int = 128,
    var seekAtStart: Boolean = false,
    var minBufferMs: Int = 15000,
    var maxBufferMs: Int = 50000,
    var hardwareDecoder: Boolean = true,
    var audioPassthrough: Boolean = true,
    var renderMode: String = "surface",
    var videoScalingMode: Int = 0,
    var customOptions: MutableMap<String, String> = mutableMapOf(),
)
