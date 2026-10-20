package com.mclone.stbemu.core.js

import android.webkit.JavascriptInterface

/**
 * Weitere Bridge-Objekte, die neben [StbJsApi] in den WebView injiziert werden.
 *
 * Die Namen auf der linken Seite sind die globalen JS-Objekte; die
 * Original-Dateien sind in Klammern genannt. Alle Methodensignaturen sind den
 * decompilierten `@JavascriptInterface`-Methoden entnommen.
 */
@Suppress("TooManyFunctions")

// ---- $$__screen (chx) ------------------------------------------------------
class ScreenJsApi(host: StbHost) : JsApiObject(host) {
    private fun w() = host.screenMetrics

    @JavascriptInterface fun GETTER_width() = w().width
    @JavascriptInterface fun GETTER_height() = w().height
    @JavascriptInterface fun GETTER_availWidth() = w().width
    @JavascriptInterface fun GETTER_availHeight() = w().height
    @JavascriptInterface fun GETTER_innerWidth() = w().width
    @JavascriptInterface fun GETTER_innerHeight() = w().height
    @JavascriptInterface fun GETTER_outerWidth() = w().width
    @JavascriptInterface fun GETTER_outerHeight() = w().height
    @JavascriptInterface fun GETTER_colorDepth() = 32
    @JavascriptInterface fun getWidth() = w().width
    @JavascriptInterface fun getHeight() = w().height
    @JavascriptInterface fun resizeTo(w: Int, h: Int) = host.resizeScreen(w, h)
    @JavascriptInterface fun moveTo(x: Int, y: Int) = Unit
}

// ---- stbWindowMgr (cja) ----------------------------------------------------
class WindowManagerJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun windowInit(url: String?) = host.windowInit(url)
    @JavascriptInterface fun windowList() = host.windowListJson()
    @JavascriptInterface fun windowInfo(id: Int) = host.windowInfoJson(id)
    @JavascriptInterface fun windowShow(id: Int) = host.windowShow(id)
    @JavascriptInterface fun windowHide(id: Int) = host.windowHide(id)
    @JavascriptInterface fun windowClose(id: Int) = host.windowClose(id)
    @JavascriptInterface fun windowAttr(id: Int, attr: String) = false
    @JavascriptInterface fun windowActive() = host.windowActiveId()
    @JavascriptInterface fun closeWindow(id: Int) = host.windowClose(id)
    @JavascriptInterface fun hideWindow(id: Int) = host.windowHide(id)
    @JavascriptInterface fun raiseWebWindow() = host.windowBringToFront()
    @JavascriptInterface fun raiseWebFaceWindow() = host.windowBringToFront()
    @JavascriptInterface fun getCurrWebUrl() = host.currentUrl()
    @JavascriptInterface fun GetFocusedInputInfo() = ""
    @JavascriptInterface fun SetFocusedInputText(text: String?) = Unit
    @JavascriptInterface fun SetVirtualKeyboardCoord(a: String?, b: Int = 0, c: Int = 0) = Unit
    @JavascriptInterface fun setVirtualKeyboardInitAttr(attr: String) = Unit
    @JavascriptInterface fun setWebFaceInitAttr(attr: String) = Unit
    @JavascriptInterface fun openWebCfgPortal(url: String) = Unit
    @JavascriptInterface fun IsScreenSaverWindowExist() = false
    @JavascriptInterface fun IsWebVkWindowExist() = false
    @JavascriptInterface fun VkSetFocus(focus: Boolean) = Unit
    @JavascriptInterface fun resizeWebWindow(id: Int, x: Int, y: Int, w: Int, h: Int) = Unit
}

// ---- stbWebWindow / stbBrowserWindow (cix) ---------------------------------
class WebWindowJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun InitWebWindow() = host.loadUrl(host.portalUrl())
    @JavascriptInterface fun InitWebWindow(url: String?) = host.loadUrl(url ?: host.portalUrl())
    @JavascriptInterface fun InitWebWindow(a: String?, b: String?) = host.loadUrl(a ?: host.portalUrl())
    @JavascriptInterface fun initWebWindow(url: String?) = InitWebWindow(url)
    @JavascriptInterface fun closeWebWindow() = host.closeWindow()
    @JavascriptInterface fun openWebWindow(url: String?) = host.loadUrl(url)
    @JavascriptInterface fun openWebWindow(a: String?, b: String?) = host.loadUrl(a)
    @JavascriptInterface fun openWebFace(url: String?) = host.loadUrl(url)
    @JavascriptInterface fun openWebFavorites() = host.loadUrl(host.portalUrl())
    @JavascriptInterface fun openWebFavorites(a: String?, b: String?) = host.loadUrl(a)
    @JavascriptInterface fun showPortalWindow() = host.loadUrl(host.portalUrl())
    @JavascriptInterface fun openBrowser(url: String?) = host.loadUrl(url)
    @JavascriptInterface fun openDownloadManager(path: String) = host.openDownloadManager(path)
    @JavascriptInterface fun LoadUrl(url: String) = host.loadUrl(url)
    @JavascriptInterface fun getCurrentUrl() = host.currentUrl()
    @JavascriptInterface fun getCurrentTitle() = host.currentTitle()
    @JavascriptInterface fun IsWebWindowExist() = true
    @JavascriptInterface fun setBookmarkImplUrl(url: String) = Unit
    @JavascriptInterface fun addBrowserBookmark() = Unit
    @JavascriptInterface fun showBrowserBookmarks() = Unit
    @JavascriptInterface fun openMediaPlayer(url: String?, kind: String?) = host.player.play(url ?: "")
    @JavascriptInterface fun setVirtualMouseMode(enable: Boolean) = Unit
    @JavascriptInterface fun windowLoad(id: Int, url: String) = host.loadUrl(url)
    @JavascriptInterface fun NavigateBack() = host.goBack()
    @JavascriptInterface fun NavigateForward() = host.goForward()
    @JavascriptInterface fun ReloadDocument() = host.reload()
    @JavascriptInterface fun StopLoading() = host.stopLoading()
    @JavascriptInterface fun SetZoomFactor(factor: Int) = host.setZoom(factor)
    @JavascriptInterface fun SetFullScreenMode(fullscreen: Boolean) = host.setFullscreen(fullscreen)
    @JavascriptInterface fun ToggleFullScreenMode() = host.setFullscreen(!host.isFullscreen())
    @JavascriptInterface fun windowId() = host.webViewId()
    @JavascriptInterface fun FocusTopWindow() = Unit
    @JavascriptInterface fun FocusMiddleWindow() = Unit
    @JavascriptInterface fun messageBroadcast(text: String) = host.broadcast(text)
    @JavascriptInterface fun messageBroadcast(text: String, target: String?) = host.broadcast(text, target)
    @JavascriptInterface fun messageSend(id: Int, text: String) = Unit
    @JavascriptInterface fun messageSend(id: Int, text: String, target: String?) = Unit
    @JavascriptInterface fun SendVirtualKeypress(key: String?, code: Int) = host.sendVirtualKey(key, code)
    @JavascriptInterface fun close() = host.closeWindow()
}

// ---- pvrManager (cif) ------------------------------------------------------
class PvrJsApi(host: StbHost) : JsApiObject(host) {
    private val pvr get() = host.pvr

    @JavascriptInterface
    fun CreateTask(url: String, name: String, fromMs: Long, toMs: Long) = pvr.createTask(url, name, fromMs, toMs)

    @JavascriptInterface
    fun CreateTask(url: String, name: String, from: String, to: String) = pvr.createTask(url, name, from, to)

    @JavascriptInterface fun GetAllTasks() = pvr.allTasksJson()
    @JavascriptInterface fun GetTasksByIDs(ids: String) = pvr.tasksByIdsJson(ids)
    @JavascriptInterface fun GetTaskByID(id: String) = pvr.taskByIdJson(id)
    @JavascriptInterface fun RemoveTask(id: String, alsoDelete: Int) = pvr.removeTask(id, alsoDelete != 0)
    @JavascriptInterface fun ChangeEndTime(id: String, end: String) = pvr.changeEndTime(id, end)
    @JavascriptInterface fun SetMaxRecordingCnt(count: Int) {
        pvr.maxRecordings = count
    }
}

// ---- timeShift (cjb) -------------------------------------------------------
class TimeShiftJsApi(host: StbHost) : JsApiObject(host) {
    private val pvr get() = host.pvr

    @JavascriptInterface fun TimeShiftOn() = pvr.timeshiftOn(true)
    @JavascriptInterface fun TimeShiftOff() = pvr.timeshiftOn(false)
    @JavascriptInterface fun SetTimeShiftFolder(path: String) {
        pvr.timeshiftFolder = path
    }
    @JavascriptInterface fun SetTimeShiftDurationMax(sec: Int) {
        pvr.timeshiftMaxSec = sec
    }
    @JavascriptInterface fun SetMaxDuration(sec: Int) = SetTimeShiftDurationMax(sec)
    @JavascriptInterface fun SetTimeShiftSlidingMode(mode: Int) {
        pvr.timeshiftSliding = mode != 0
    }
    @JavascriptInterface fun SetSlidingMode(enabled: Boolean) {
        pvr.timeshiftSliding = enabled
    }
    @JavascriptInterface fun EnterTimeShift() = pvr.timeshiftEnter(0)
    @JavascriptInterface fun ExitTimeShift() = pvr.timeshiftExit()
    @JavascriptInterface fun ExitTimeShiftAndSave() = pvr.timeshiftExitAndSave(null, null, 0)
    @JavascriptInterface fun ExitTimeShiftAndSaveDuration(a: String?, b: String?, sec: Int) = pvr.timeshiftExitAndSave(a, b, sec)
    @JavascriptInterface fun TimeShiftEnter(sec: Int) = pvr.timeshiftEnter(sec)
    @JavascriptInterface fun TimeShiftExit() = pvr.timeshiftExit()
    @JavascriptInterface fun TimeShiftExitAndSave(a: String?, b: String?) = pvr.timeshiftExitAndSave(a, b, 0)
    @JavascriptInterface fun TimeShiftExitAndSaveDuration(a: String?, b: String?, sec: Int) = pvr.timeshiftExitAndSave(a, b, sec)
}

// ---- stbDownloadManager (cij) ----------------------------------------------
class DownloadManagerJsApi(host: StbHost) : JsApiObject(host) {
    private val dl get() = host.downloads

    @JavascriptInterface fun AddJob(url: String, name: String?) = dl.addJob(url, name)
    @JavascriptInterface fun AddMeasureJob(url: String) = dl.addMeasureJob(url)
    @JavascriptInterface fun StartJob(id: Double) = dl.startJob(id.toLong())
    @JavascriptInterface fun StopJob(id: Double) = dl.stopJob(id.toLong())
    @JavascriptInterface fun DeleteJob(id: Double, alsoFile: Boolean) = dl.deleteJob(id.toLong(), alsoFile)
    @JavascriptInterface fun AdjustJobPriority(id: Double, up: Boolean) = dl.adjustPriority(id.toLong(), up)
    @JavascriptInterface fun PlayDownloadedMedia(id: Double) = dl.playDownloaded(id.toLong())
    @JavascriptInterface fun GetQueueInfo() = dl.queueInfoJson()
    @JavascriptInterface fun GetQueueInfo(name: String) = dl.queueInfoJson(name)
    @JavascriptInterface fun GetMeasureInfo() = dl.measureInfoJson()
    @JavascriptInterface fun RestoreJobs(path: String) = dl.restoreJobs(path)
    @JavascriptInterface fun InvalidateCatalog(path: String) = Unit
}

// ---- stbUpdate (ciu) -------------------------------------------------------
class UpdateJsApi(host: StbHost) : JsApiObject(host) {
    private val upd get() = host.updater

    @JavascriptInterface fun init() = Unit
    @JavascriptInterface fun getStatus() = upd.status
    @JavascriptInterface fun getStatusStr() = upd.statusText
    @JavascriptInterface fun getPercents() = upd.progress
    @JavascriptInterface fun eventProgress() = upd.emitProgress()
    @JavascriptInterface fun eventStateChanged() = upd.emitStateChanged()
    @JavascriptInterface fun startCheck(url: String?) = upd.check(url)
    @JavascriptInterface fun startUpdate(flag: Int, url: String?) = upd.startUpdate(url)
    @JavascriptInterface fun startAutoUpdate(url: String?, checkOnly: Boolean) = upd.startAutoUpdate(url, checkOnly)
    @JavascriptInterface fun setAutoUpdateInitAttr(attr: String) = upd.autoUpdateAttr = attr
    @JavascriptInterface fun getActiveBank() = "A"
    @JavascriptInterface fun GetFlashBankCount() = 2
    @JavascriptInterface fun getImageVersionStr() = upd.imageVersion
    @JavascriptInterface fun getImageDateStr() = upd.imageDate
    @JavascriptInterface fun getImageDescStr() = upd.imageDescription
}

// ---- stbStorage (cir) ------------------------------------------------------
class StorageJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun clear() = host.storage.clear()
    @JavascriptInterface fun key(index: Int) = host.storage.key(index) ?: ""
    @JavascriptInterface fun setItem(key: String, value: String) {
        host.storage.setItem(key, value)
    }
    @JavascriptInterface fun removeItem(key: String) {
        host.storage.removeItem(key)
    }
    @JavascriptInterface fun getItem(key: String) = host.storage.getItem(key) ?: ""
}

// ---- stbUPnP (cis) ---------------------------------------------------------
class UpnpJsApi(host: StbHost) : JsApiObject(host) {
    private val upnp get() = host.upnp
    @JavascriptInterface fun init() = Unit
    @JavascriptInterface fun openServer(name: String) = upnp.startServer(name)
    @JavascriptInterface fun openContext(context: String) = upnp.context = context
    @JavascriptInterface fun setFilterName(name: String) {
        upnp.filterName = name
    }
    @JavascriptInterface fun setFilterType(type: Int) {
        upnp.filterType = type
    }
    @JavascriptInterface fun openPage(page: Int) = upnp.openPage(page)
}

// ---- stbNfs (cin) ----------------------------------------------------------
class NfsJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun ListDir(path: String) = host.listDirectory(path, true)
    @JavascriptInterface fun IsFileExist(path: String) = host.fileExists(path)
}

// ---- stbDisplayManager (cii) -----------------------------------------------
class DisplayManagerJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GETTER_JSON_list() = "[]"
}

// ---- stbAudioSource0 (cig) -------------------------------------------------
class AudioSourceJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GETTER_source() = 0
    @JavascriptInterface fun GETTER_volume() = host.player.volume
    @JavascriptInterface fun SETTER_volume(volume: Int) {
        host.player.volume = volume
    }
}

// ---- stbPlayerSurface0 (ciq) -----------------------------------------------
class PlayerSurfaceJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GETTER_id() = 0
    @JavascriptInterface fun GETTER_type() = 0
    @JavascriptInterface fun GETTER_opacity() = 1.0f
    @JavascriptInterface fun SETTER_opacity(value: Float) = Unit
    @JavascriptInterface fun moveUp() = true
    @JavascriptInterface fun moveDown() = true
    @JavascriptInterface fun moveTop() = true
    @JavascriptInterface fun moveBottom() = true
}

// ---- stbWebBrowserSurface0 (civ) -------------------------------------------
class WebSurfaceJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GETTER_id() = 1
    @JavascriptInterface fun GETTER_type() = 1
    @JavascriptInterface fun GETTER_opacity() = 1.0f
    @JavascriptInterface fun SETTER_opacity(value: Float) = Unit
    @JavascriptInterface fun moveUp() = true
    @JavascriptInterface fun moveDown() = true
    @JavascriptInterface fun moveTop() = true
    @JavascriptInterface fun moveBottom() = true
}

// ---- stbPlayerManager (cip) ------------------------------------------------
class PlayerManagerJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GETTER_JSON_list() = host.player.listJson()
    @JavascriptInterface fun GETTER_JSON_statistics() = host.player.statisticsJson()
    @JavascriptInterface fun GETTER_JSON_capabilities() = host.player.capabilitiesJson()
}

// ---- epgManager (cia) ------------------------------------------------------
class EpgJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GetEPGBrief(args: String) = host.epg.brief(args)
    @JavascriptInterface fun GetEPGSchedule(args: String) = host.epg.schedule(args)
    @JavascriptInterface fun GetEPGSchedule(args: String, day: Int) = host.epg.schedule(args, day)
    @JavascriptInterface fun GetChannelList(args: String) = host.epg.channelList(args)
    @JavascriptInterface fun GetChannelList() = host.epg.channelList("")
    @JavascriptInterface fun GetSupportedScanTypes() = host.epg.supportedScanTypes()
    @JavascriptInterface fun GetCurrentScanTypes(args: String) = host.epg.currentScanTypes(args)
    @JavascriptInterface fun GetCurrentScanTypes() = host.epg.currentScanTypes("")
    @JavascriptInterface fun StartChannelScan(mode: Int) = 0
    @JavascriptInterface fun StartChannelScanManual(mode: Int, frequency: Int, symbolRate: Int, modulation: String?, type: String?) = 0
    @JavascriptInterface fun StartChannelScanManual(params: String) = 0
    @JavascriptInterface fun StopChannelScan() = 0
    @JavascriptInterface fun StopChannelScan(args: String) = 0
    @JavascriptInterface fun TuneChannel(channel: String) = 0
    @JavascriptInterface fun TuneChannel(frequency: Int, symbolRate: Int) = 0
    @JavascriptInterface fun UnTuneChannel() = 0
    @JavascriptInterface fun UnTuneChannel(channel: String) = 0
    @JavascriptInterface fun ClearChannelList() = 0
    @JavascriptInterface fun RemoveChannel(id: Int) = 0
    @JavascriptInterface fun SetAntennaPower(on: Int) = 0
    @JavascriptInterface fun SetAntennaPower(on: Int, power: Int) = 0
    @JavascriptInterface fun GetAntennaPower() = false
    @JavascriptInterface fun GetAntennaPower(args: String) = false
    @JavascriptInterface fun SetScanParams(params: String) = Unit
}

// ---- cookieManager (chu, nur SDK 19) ---------------------------------------
class CookieManagerJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun getCookie(url: String) = host.cookieManager.getCookie(url) ?: ""
    @JavascriptInterface fun setCookie(url: String, cookie: String) {
        host.cookieManager.setCookie(url, cookie)
    }
}

// ---- stbPlayer0 (cio) - Eigenschaften-Zugriff des Players -------------------
class PlayerJsApi(host: StbHost) : JsApiObject(host) {
    private val p get() = host.player

    @JavascriptInterface fun GETTER_id() = 0
    @JavascriptInterface fun GETTER_number() = 0
    @JavascriptInterface fun GETTER_JSON_capabilities() = p.capabilitiesJson()

    @JavascriptInterface fun GETTER_source() = if (p.isPlaying()) 1 else 0
    @JavascriptInterface fun GETTER_state() = if (p.isPlaying()) 1 else 0

    @JavascriptInterface fun GETTER_mute() = p.muted
    @JavascriptInterface fun SETTER_mute(value: Int) { p.muted = value != 0 }
    @JavascriptInterface fun GETTER_volume() = p.volume
    @JavascriptInterface fun SETTER_volume(value: Int) { p.volume = value }

    @JavascriptInterface fun GETTER_audioPID() = p.audioTrackPid
    @JavascriptInterface fun SETTER_audioPID(pid: Int) = p.selectAudioTrack(pid)
    @JavascriptInterface fun GETTER_JSON_audioTracks() = "[]"
    @JavascriptInterface fun GETTER_JSON_audioLanguages() = "[]"
    @JavascriptInterface fun GETTER_dolbyDigitalAudioMode() = 0
    @JavascriptInterface fun GETTER_audioPrimaryLanguage() = host.settings.audioPrimary
    @JavascriptInterface fun SETTER_audioPrimaryLanguage(lang: String) { host.settings.audioPrimary = lang }
    @JavascriptInterface fun GETTER_audioSecondaryLanguage() = host.settings.audioSecondary
    @JavascriptInterface fun SETTER_audioSecondaryLanguage(lang: String) { host.settings.audioSecondary = lang }
    @JavascriptInterface fun GETTER_JSON_audioLanguagesAll() = "[]"

    @JavascriptInterface fun SETTER_audioLanguages(langs: String) { host.settings.audioPrimary = langs }

    @JavascriptInterface fun GETTER_JSON_subtitleLanguages() = "[]"
    @JavascriptInterface fun SETTER_subtitleLanguages(langs: String) { host.settings.subtitlePrimary = langs }
    @JavascriptInterface fun GETTER_subtitlesPrimaryLanguage() = host.settings.subtitlePrimary
    @JavascriptInterface fun SETTER_subtitlesPrimaryLanguage(lang: String) { host.settings.subtitlePrimary = lang }
    @JavascriptInterface fun GETTER_subtitlesSecondaryLanguage() = host.settings.subtitleSecondary
    @JavascriptInterface fun SETTER_subtitlesSecondaryLanguage(lang: String) { host.settings.subtitleSecondary = lang }
    @JavascriptInterface fun GETTER_enableSubtitles() = p.subtitlesEnabled
    @JavascriptInterface fun SETTER_enableSubtitles(on: Boolean) { p.subtitlesEnabled = on }
    @JavascriptInterface fun GETTER_subtitlesPID() = p.subtitleTrackPid
    @JavascriptInterface fun SETTER_subtitlesPID(pid: Int) = p.selectSubtitleTrack(pid)
    @JavascriptInterface fun GETTER_JSON_subtitlesTracks() = "[]"
    @JavascriptInterface fun GETTER_subtitlesColor() = p.subtitleColor
    @JavascriptInterface fun SETTER_subtitlesColor(color: Int) { p.subtitleColor = color }
    @JavascriptInterface fun GETTER_subtitlesEncoding() = p.subtitleEncoding
    @JavascriptInterface fun SETTER_subtitlesEncoding(enc: String) { p.subtitleEncoding = enc }
    @JavascriptInterface fun GETTER_subtitlesFont() = p.subtitleFont
    @JavascriptInterface fun SETTER_subtitlesFont(font: String) { p.subtitleFont = font }
    @JavascriptInterface fun GETTER_subtitlesOffset() = p.subtitleOffset
    @JavascriptInterface fun SETTER_subtitlesOffset(v: Int) { p.subtitleOffset = v }
    @JavascriptInterface fun GETTER_subtitlesSize() = p.subtitleSize
    @JavascriptInterface fun SETTER_subtitlesSize(v: Int) { p.subtitleSize = v }

    @JavascriptInterface fun GETTER_bufferLoad() = p.bufferLoadPercent
    @JavascriptInterface fun GETTER_duration() = p.durationSec
    @JavascriptInterface fun GETTER_durationMs() = p.durationSec * 1000L
    @JavascriptInterface fun GETTER_position() = p.positionSec
    @JavascriptInterface fun GETTER_positionMs() = p.positionSec * 1000L
    @JavascriptInterface fun GETTER_positionPercent() = p.positionPercent.toFloat()
    @JavascriptInterface fun GETTER_speed() = p.speedPercent / 100f
    @JavascriptInterface fun GETTER_JSON_speeds() = "[0.25,0.5,1.0,1.5,2.0]"

    @JavascriptInterface fun GETTER_loop() = false
    @JavascriptInterface fun SETTER_loop(on: Boolean) = p.setLoop(if (on) 1 else 0)

    @JavascriptInterface fun GETTER_JSON_HLSInfo() = p.hlsInfoJson()
    @JavascriptInterface fun GETTER_JSON_metadataInfo() = p.metadataJson()
    @JavascriptInterface fun GETTER_JSON_videoInfo() = p.videoInfoJson()
    @JavascriptInterface fun GETTER_JSON_statistics() = p.statisticsJson()

    @JavascriptInterface fun GETTER_fullscreen() = host.isFullscreen()
    @JavascriptInterface fun SETTER_fullscreen(on: Boolean) = host.setFullscreen(on)
    @JavascriptInterface fun GETTER_videoWindowMode() = 0
    @JavascriptInterface fun SETTER_videoWindowMode(mode: Int) = Unit
    @JavascriptInterface fun GETTER_aspectConversion() = p.aspectMode
    @JavascriptInterface fun SETTER_aspectConversion(mode: Int) { p.aspectMode = mode }
    @JavascriptInterface fun GETTER_conversionMode3D() = 0

    @JavascriptInterface fun play(url: String) = p.play(url)
    @JavascriptInterface fun stop() = p.stop()
    @JavascriptInterface fun pause() = p.pause()
    @JavascriptInterface fun resume() = p.resume()
    @JavascriptInterface fun setViewport(x: Int, y: Int, width: Int, height: Int) = p.setViewport(x, y, width, height)
    @JavascriptInterface fun setViewport(json: String) = Unit

    @JavascriptInterface fun GETTER_multicastProxy() = ""
    @JavascriptInterface fun SETTER_multicastProxy(url: String) = Unit
}

// ---- stbBrowser (cih) - Anwendungs-Start und App-Steuerung ------------------
class BrowserJsApi(host: StbHost) : JsApiObject(host) {
    @JavascriptInterface fun GETTER_JSON_list() = "[]"
}
