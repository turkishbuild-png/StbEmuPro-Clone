package com.mclone.stbemu.core.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONObject

/**
 * Standard-Player auf Android-Basis (MediaPlayer).
 *
 * Das Original nutzt fuer "internal" den eingebetteten `libplayer.so`
 * (26 KB) plus FFmpeg-Bibliotheken. Der Klon ersetzt das durch
 * MediaPlayer und verliert dadurch nur exotische Codecs.
 */
class AndroidPlayerBackend(private val context: Context) : PlayerBackend {

    override val id = "internal"
    override val displayName = "Interner Player"
    override val available: Boolean get() = true

    private var player: MediaPlayer? = null
    private var surface: Surface? = null

    override fun prepare(context: Context) = Unit

    override fun attach(surface: Surface) {
        this.surface = surface
        player?.setSurface(surface)
    }

    override fun setVideoScalingMode(mode: Int) {
        player?.setVideoScalingMode(mode)
    }

    @Suppress("DEPRECATION")
    override fun play(url: String, headers: Map<String, String>) {
        if (url.isBlank()) {
            pause()
            return
        }
        release()
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            surface?.let(mp::setSurface)
            val uri = Uri.parse(url)
            headers.forEach { (k, v) -> mp.setDataSource(context, Uri.parse(url), emptyMap<String, String>().also { it[k] = v }) }
            if (headers.isEmpty()) mp.setDataSource(context, uri)
            mp.prepareAsync()
            mp.setOnPreparedListener { it.start() }
        }
    }

    override fun stop() {
        runCatching { player?.stop() }
    }

    override fun release() {
        runCatching { player?.release() }
        player = null
    }

    override val isPlaying: Boolean get() = runCatching { player?.isPlaying == true }.getOrDefault(false)
    override val positionMs: Long get() = runCatching { player?.currentPosition?.toLong() ?: 0 }.getOrDefault(0)
    override val durationMs: Long get() = runCatching { player?.duration?.toLong() ?: 0 }.getOrDefault(0)
    override val bufferedPercent: Int get() = runCatching {
        player?.bufferedPercentage ?: 0
    }.getOrDefault(0)

    override fun seekTo(positionMs: Long) {
        runCatching { player?.seekTo(positionMs.toInt()) }
    }

    override fun setVolume(volume: Int) {
        val v = volume.coerceIn(0, 100) / 100f
        runCatching { player?.setVolume(v, v) }
    }

    override fun setMuted(muted: Boolean) {
        runCatching { player?.setVolume(0f, 0f) }
    }

    override fun setSpeed(percent: Int) {
        runCatching {
            player?.playbackParams = player?.playbackParams?.setSpeed(percent / 100f)
        }
    }

    override val videoWidth: Int get() = runCatching { player?.videoWidth ?: 0 }.getOrDefault(0)
    override val videoHeight: Int get() = runCatching { player?.videoHeight ?: 0 }.getOrDefault(0)

    override fun mediaInfo(): JSONObject = JSONObject()
}

/**
 * ExoPlayer / Media3 - Standardbackend ab Android 21.
 *
 * Das Original bindet ExoPlayer2 ueber `com.google.android.exoplayer2`
 * ein und registriert es unter dem Schluessel "exo".
 */
class ExoPlayerBackend(private val context: Context) : PlayerBackend {

    override val id = "exo"
    override val displayName = "ExoPlayer"
    override val available: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    private var player: ExoPlayer? = null
    private var scalingMode = 0

    override fun prepare(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
    }

    override fun attach(surface: Surface) {
        prepare(context)
        player?.setVideoSurface(surface)
    }

    override fun setVideoScalingMode(mode: Int) {
        scalingMode = mode
    }

    override fun play(url: String, headers: Map<String, String>) {
        prepare(context)
        val exo = player ?: return
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(url.substringAfterLast('/'))
                    .build()
            )
            .build()
        exo.setMediaItem(item)
        exo.setVideoScalingMode(
            when (scalingMode) {
                1 -> androidx.media3.exoplayer.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                2 -> androidx.media3.exoplayer.AspectRatioFrameLayout.RESIZE_MODE_FILL
                else -> androidx.media3.exoplayer.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun stop() {
        player?.stop()
    }

    override fun release() {
        player?.release()
        player = null
    }

    override val isPlaying: Boolean get() = player?.isPlaying == true
    override val positionMs: Long get() = player?.currentPosition ?: 0
    override val durationMs: Long get() = player?.duration ?: 0
    override val bufferedPercent: Int
        get() = player?.let {
            val total = it.duration
            if (total <= 0) 0 else (it.bufferedPosition * 100 / total).toInt()
        } ?: 0

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    override fun setVolume(volume: Int) {
        player?.volume = volume.coerceIn(0, 100) / 100f
    }

    override fun setMuted(muted: Boolean) {
        player?.volume = if (muted) 0f else currentVolume
    }

    private var currentVolume = 1f

    override fun setSpeed(percent: Int) {
        player?.setPlaybackSpeed(percent / 100f)
    }

    override val videoWidth: Int get() = player?.videoSize?.width ?: 0
    override val videoHeight: Int get() = player?.videoSize?.height ?: 0

    override fun mediaInfo(): JSONObject = JSONObject().apply {
        player?.let {
            put("audio", if (it.audioFormat != null) 1 else 0)
            put("video", if (it.videoSize != null) 1 else 0)
            put("VideoWidth", it.videoSize?.width ?: 0)
            put("VideoHeight", it.videoSize?.height ?: 0)
        }
        put("MimeTypes", JSON_MIME_SUPPORT)
    }

    companion object {
        val JSON_MIME_SUPPORT: List<String> = listOf(
            MimeTypes.VIDEO_MP4,
            MimeTypes.VIDEO_H264,
            MimeTypes.VIDEO_H265,
            MimeTypes.VIDEO_WEBM,
            MimeTypes.VIDEO_MPEG2TS,
            MimeTypes.APPLICATION_M3U8,
            MimeTypes.APPLICATION_MPD,
            MimeTypes.AUDIO_MPEG,
            MimeTypes.AUDIO_AAC,
            MimeTypes.TEXT_VTT,
            MimeTypes.TEXT_SUBRIP,
            "application/x-subrip",
            "text/x-ssa",
        )
    }
}

/**
 * IJKPlayer-Backend.
 *
 * Das Original liefert `libijkffmpeg.so` (9,6 MB), `libijkplayer.so` und
 * `libijksdl.so` fuer armeabi-v7a mit und registriert sie unter "ijk".
 * IJK ist weiterhin der einzige Weg fuer MPEG-TS-Multicast und manche
 * russische Streams - im Klon als separates Modul vorgesehen.
 */
class IjkPlayerBackend(private val context: Context) : PlayerBackend {

    override val id = "ijk"
    override val displayName = "IJKPlayer"
    override val available: Boolean get() = IjkRuntime.available

    private var handle: IjkRuntime.Handle? = null

    override fun prepare(context: Context) = Unit

    override fun attach(surface: Surface) {
        handle?.surface = surface
    }

    override fun setVideoScalingMode(mode: Int) {
        handle?.scalingMode = mode
    }

    override fun play(url: String, headers: Map<String, String>) {
        if (url.isBlank()) return
        handle = IjkRuntime.create(context, url, headers)
        handle?.surface?.let(::attach)
    }

    override fun stop() {
        handle?.stop()
    }

    override fun release() {
        handle?.release()
        handle = null
    }

    override val isPlaying: Boolean get() = handle?.isPlaying == true
    override val positionMs: Long get() = handle?.positionMs ?: 0
    override val durationMs: Long get() = handle?.durationMs ?: 0
    override val bufferedPercent: Int get() = handle?.bufferedPercent ?: 0

    override fun seekTo(positionMs: Long) {
        handle?.seekTo(positionMs)
    }

    override fun setVolume(volume: Int) {
        handle?.volume = volume
    }

    override fun setMuted(muted: Boolean) {
        handle?.muted = muted
    }

    override fun setSpeed(percent: Int) {
        handle?.speed = percent / 100f
    }

    override val videoWidth: Int get() = handle?.videoWidth ?: 0
    override val videoHeight: Int get() = handle?.videoHeight ?: 0

    override fun mediaInfo(): JSONObject = JSONObject()
}

/**
 * mpv-Backend.
 *
 * Das Original liefert `libmpv.so` (2,3 MB) und registriert es unter "mpv",
 * aber nur ab API 21 (`MainActivity.onCreate`: `if (SDK_INT >= 21)`).
 */
class MpvPlayerBackend(private val context: Context) : PlayerBackend {

    override val id = "mpv"
    override val displayName = "MPV"
    override val available: Boolean get() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && MpvRuntime.available

    private var handle: MpvRuntime.Handle? = null

    override fun prepare(context: Context) = Unit

    override fun attach(surface: Surface) {
        handle?.surface = surface
    }

    override fun setVideoScalingMode(mode: Int) = Unit

    override fun play(url: String, headers: Map<String, String>) {
        if (url.isBlank()) return
        handle = MpvRuntime.create(context, url)
        handle?.surface?.let(::attach)
    }

    override fun stop() {
        handle?.stop()
    }

    override fun release() {
        handle?.release()
        handle = null
    }

    override val isPlaying: Boolean get() = handle?.isPlaying == true
    override val positionMs: Long get() = handle?.positionMs ?: 0
    override val durationMs: Long get() = handle?.durationMs ?: 0
    override val bufferedPercent: Int get() = handle?.bufferedPercent ?: 0

    override fun seekTo(positionMs: Long) {
        handle?.seekTo(positionMs)
    }

    override fun setVolume(volume: Int) {
        handle?.volume = volume
    }

    override fun setMuted(muted: Boolean) {
        handle?.muted = muted
    }

    override fun setSpeed(percent: Int) {
        handle?.speed = percent / 100f
    }

    override val videoWidth: Int get() = handle?.videoWidth ?: 0
    override val videoHeight: Int get() = handle?.videoHeight ?: 0

    override fun mediaInfo(): JSONObject = JSONObject()
}
