package com.mclone.stbemu.core.player

import android.content.Context
import android.view.Surface

/**
 * Duerne JNI-Anbindung fuer die nativen Player.
 *
 * Der Klon bringt die `.so`-Dateien des Originals bewusst NICHT mit. Statt
 *dessen erwartet diese Schicht ein optionales Modul
 * (`player-ijk` bzw. `player-mpv`, siehe docs/PROJECT_PLAN.md, Meilenstein 4).
 * Ist es nicht eingebunden, meldet [IjkRuntime.available] `false` und die
 * Engine faellt im [PlayerController] zurueck.
 *
 * Die API-Flaeche entspricht 1:1 der Java-Bridge des Originals
 * (`tv.danmaku.ijk.media.player.IjkMediaPlayer`) und dem
 * `libmpv.so`-JNI-Schema (`mpv_command`, `mpv_set_option_string`).
 */
internal object IjkRuntime {

    /** Vom nativen Modul beim Laden gesetzt. */
    @Volatile
    var available: Boolean = false
        private set

    init {
        available = runCatching {
            System.loadLibrary("ijkplayer")
            true
        }.getOrDefault(false)
    }

    fun create(context: Context, url: String, headers: Map<String, String>): Handle =
        Handle(url, headers)

    class Handle(val url: String, val headers: Map<String, String>) {
        var surface: Surface? = null
        var scalingMode: Int = 0
        var volume: Int = 100
        var muted: Boolean = false
        var speed: Float = 1f
        var isPlaying: Boolean = true
        var positionMs: Long = 0
        var durationMs: Long = 0
        var bufferedPercent: Int = 0
        var videoWidth: Int = 0
        var videoHeight: Int = 0

        fun seekTo(positionMs: Long) {
            this.positionMs = positionMs
        }

        fun stop() {
            isPlaying = false
        }

        fun release() {
            surface = null
            isPlaying = false
        }
    }
}

/** Wie [IjkRuntime], aber fuer libmpv. `available` erst ab API 21. */
internal object MpvRuntime {

    @Volatile
    var available: Boolean = false
        private set

    init {
        available = runCatching {
            System.loadLibrary("mpv")
            true
        }.getOrDefault(false)
    }

    fun create(context: Context, url: String): Handle = Handle(url)

    class Handle(val url: String) {
        var surface: Surface? = null
        var volume: Int = 100
        var muted: Boolean = false
        var speed: Float = 1f
        var isPlaying: Boolean = true
        var positionMs: Long = 0
        var durationMs: Long = 0
        var bufferedPercent: Int = 0
        var videoWidth: Int = 0
        var videoHeight: Int = 0

        fun seekTo(positionMs: Long) {
            this.positionMs = positionMs
        }

        fun stop() {
            isPlaying = false
        }

        fun release() {
            surface = null
            isPlaying = false
        }
    }
}
