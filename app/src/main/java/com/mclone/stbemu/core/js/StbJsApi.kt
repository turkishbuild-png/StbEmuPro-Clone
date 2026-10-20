package com.mclone.stbemu.core.js

import android.webkit.JavascriptInterface

/**
 * Das zentrale STB-Objekt - in JavaScript als `stb` und `gSTB` verfuegbar.
 *
 * Ein echter Infomir-MAG-Receiver stellt genau diese flache API bereit; die
 * Original-App bildet sie ueber eine vierstufige Vererbungskette ab
 * (`cib` -> `cic` -> `cid` -> `cie`), wobei `cie` als "stb"/"gSTB" registriert
 * wird (`p000.cjp.mo6955a`). Der Klon buendelt die Kette in eine Klasse.
 *
 * Es handelt sich um die groesste Einzelkomponente der App: rund 230 Methoden
 * in vier Gruppen - Player, Geraete/Identitaet, Portal-/Netzwerk und
 * Bildschirm/TV-Tuner.
 *
 * Konvention: `SET_*` schreibt in den Portal-State, `Get*` liest daraus,
 * `GETTER_*`/`SETTER_*` (Konvention des Herstellers, Schreibweise beibehalten)
 * liefert/setzt Eigenschaften des Players.
 */
@Suppress("TooManyFunctions", "LargeClass")
class StbJsApi(host: StbHost) : JsApiObject(host) {

    private val player get() = host.player
    private val net get() = host.network
    private val cfg get() = host.settings

    // =====================================================================
    // Initialisierung / Steuerung
    // =====================================================================

    @JavascriptInterface
    fun InitPlayer() {
        player.attach(host.context)
        player.play()
    }

    @JavascriptInterface
    fun DeinitPlayer() = player.release()

    /** Wie im Original (`p000.cib.InitPlayer`): Player anlegen, Zustand PLAYING. */
    @JavascriptInterface
    fun InitPlayerEx() {
        player.attach(host.context)
        player.play()
    }

    /** Startet einen Stream. Leere Quelle stoppt. */
    @JavascriptInterface
    @JvmOverloads
    fun Play(url: String, channel: String = "") {
        if (url.isBlank()) {
            player.stop()
            return
        }
        player.play(url)
    }

    @JavascriptInterface
    fun Stop() = player.stop()

    @JavascriptInterface
    fun Pause() = player.pause()

    @JavascriptInterface
    fun Continue() = player.resume()

    @JavascriptInterface
    fun Step() {
        // Von der Firmware angefordert, in dieser Android-Portalgeneration
        // ohne Wirkung (kein Einzelbilddecoder).
    }

    @JavascriptInterface
    fun StandBy(enable: Boolean) {
        if (enable) player.stop()
    }

    @JavascriptInterface
    fun IgnoreUpdates(ignore: Boolean) = Unit // Update-Sperre wird vom Portal selbst verwaltet

    @JavascriptInterface
    fun SetInternalPortalActive(active: Boolean) = Unit

    @JavascriptInterface
    fun IsInternalPortalActive(): Boolean = false

    @JavascriptInterface
    fun IsPlaying(): Boolean = player.isPlaying

    @JavascriptInterface
    fun IsWebWindowExist(): Boolean = true

    @JavascriptInterface
    fun Version(): String = host.firmware.firmwareVersion

    @JavascriptInterface
    fun ExecAction(action: String): Boolean = false

    // =====================================================================
    // Transport / Position
    // =====================================================================

    @JavascriptInterface fun GetPosTime(): Int = player.positionSec

    @JavascriptInterface fun GetPosTimeEx(): Int = player.positionSec
    @JavascriptInterface fun GetPosPercent(): Int = player.positionPercent

    @JavascriptInterface
    fun GetPosPercentEx(): Int = player.positionPercent

    @JavascriptInterface fun GetMediaLen(): Int = player.durationSec
    @JavascriptInterface fun GetMediaLenEx(): Int = player.durationSec

    @JavascriptInterface
    fun SetPosTime(seconds: Int): Int {
        player.seekTo((player.durationSec * seconds / 100).coerceAtLeast(0), player.durationSec)
        return 0
    }

    @JavascriptInterface
    fun SetPosTimeEx(seconds: Int): Int = SetPosTime(seconds)

    /** Format "hh:mm:ss[:mmm]" - von neueren Portalen bevorzugt. */
    @JavascriptInterface
    fun GetPosStr(): String = player.positionStr

    @JavascriptInterface
    fun SetPosStr(value: String): Int {
        val target = parseTime(value) ?: return -1
        player.seekTo(target, player.durationSec)
        return 0
    }

    @JavascriptInterface
    fun SetPos(time: String): Int = SetPosStr(time)

    @JavascriptInterface
    fun SetPosPercent(percent: Int): Int {
        player.seekTo(percent, 100)
        return 0
    }

    @JavascriptInterface
    fun SetPosPercentEx(percent: Int): Int = SetPosPercent(percent)

    @JavascriptInterface
    fun SetLoop(mode: Int) = player.setLoop(mode)

    @JavascriptInterface
    fun SetSpeed(speed: Int): Int = player.setSpeed(speed)

    @JavascriptInterface fun GetSpeed(): Int = player.speedPercent

    // =====================================================================
    // Audio
    // =====================================================================

    @JavascriptInterface
    fun SetVolume(volume: Int): Int {
        player.volume = volume
        return 0
    }

    @JavascriptInterface fun GetVolume(): Int = player.volume

    @JavascriptInterface
    fun SetMute(muted: Int) {
        player.muted = muted != 0
    }

    @JavascriptInterface
    fun GetMute(): Int = if (player.muted) 1 else 0

    @JavascriptInterface
    fun SetAudioOperationalMode(mode: Int) {
        cfg.audioOperationalMode = mode
    }

    @JavascriptInterface
    fun SetHDMIAudioOut(mode: Int) {
        cfg.hdmiAudioOut = mode
    }

    @JavascriptInterface
    fun SetDRC(on: Int, level: Int) {
        cfg.drcEnabled = on != 0
        cfg.drcLevel = level
    }

    @JavascriptInterface
    fun SetStereoMode(mode: Int) {
        cfg.stereoMode = mode
    }

    @JavascriptInterface
    fun SetAudioLangs(primary: String, secondary: String) {
        cfg.audioPrimary = primary
        cfg.audioSecondary = secondary
        player.selectAudioLanguage(primary)
    }

    @JavascriptInterface
    fun SetSubtitleLangs(primary: String, secondary: String) {
        cfg.subtitlePrimary = primary
        cfg.subtitleSecondary = secondary
        player.selectSubtitleLanguage(primary)
    }

    @JavascriptInterface
    fun SetAudioPID(pid: Int) {
        player.selectAudioTrack(pid)
    }

    @JavascriptInterface fun GetAudioPID(): Int = player.audioTrackPid

    @JavascriptInterface
    fun SetSubtitlePID(pid: Int) {
        player.selectSubtitleTrack(pid)
    }

    @JavascriptInterface fun GetSubtitlePID(): Int = player.subtitleTrackPid

    @JavascriptInterface
    fun SetCASType(type: Int) = Unit // CAS wird nicht emuliert (kein Entschlüsselungsmodul)

    @JavascriptInterface
    fun SetCASDescrambling(on: Int) = Unit

    @JavascriptInterface
    fun SetScramblingType(type: Int) = 0

    @JavascriptInterface
    fun SetScramblingKey(key: String?, type: Int) = 0

    @JavascriptInterface
    fun SetCASParam(a: String?, b: Int, c: String?, d: Int, e: Int) = Unit

    @JavascriptInterface
    fun SetupWebCAS(url: String, params: String) = Unit

    @JavascriptInterface
    fun SetComponentMode(mode: Int) = Unit

    @JavascriptInterface
    fun SetupSPdif(enabled: Int) = Unit

    // =====================================================================
    // Untertitel
    // =====================================================================

    @JavascriptInterface
    fun SetSubtitles(enabled: Boolean) {
        player.subtitlesEnabled = enabled
    }

    @JavascriptInterface
    fun SetSubtitlesSize(size: Int) {
        player.subtitleSize = size
    }

    @JavascriptInterface
    fun SetSubtitleFont(font: String) {
        player.subtitleFont = font
    }

    @JavascriptInterface
    fun SetSubtitlePos(pos: Int) {
        player.subtitlePosition = pos
    }

    @JavascriptInterface
    fun SetSubtitleColor(color: Int) {
        player.subtitleColor = color
    }

    @JavascriptInterface
    fun SetSubtitlesColor(color: Int) = SetSubtitleColor(color)

    @JavascriptInterface
    fun SetSubtitlesOffs(offsets: Int) {
        player.subtitleOffset = offsets
    }

    @JavascriptInterface
    fun SetSubtitlesEncoding(encoding: String) {
        player.subtitleEncoding = encoding
    }

    @JavascriptInterface
    fun SetSubtitlesLang(lang: String) {
        player.subtitleLanguage = lang
    }

    @JavascriptInterface
    fun LoadExternalSubtitles(path: String) {
        player.loadExternalSubtitle(path)
    }

    @JavascriptInterface
    fun ShowSubtitle(x: Int, y: Int, text: String?) {
        player.showSubtitleOverlay(x, y, text)
    }

    @JavascriptInterface
    fun SetSyncCorrection(pts: Int, program: Int) {
        player.syncOffsetMs = pts
    }

    @JavascriptInterface
    fun SetSyncOffsetCorrection(offset: Int) {
        player.syncOffsetMs = offset
    }

    @JavascriptInterface
    fun SetSyncOffsetForRT(offset: Int) {
        player.syncOffsetMs = offset
    }

    @JavascriptInterface fun GetSyncOffsetForRT(): Int = player.syncOffsetMs

    // =====================================================================
    // Bild / Darstellung
    // =====================================================================

    @JavascriptInterface
    fun SetAspect(aspect: Int): Int {
        player.aspectMode = aspect
        return 0
    }

    @JavascriptInterface fun GetAspect(): Int = player.aspectMode

    @JavascriptInterface fun SetMode(mode: Int) = Unit

    @JavascriptInterface
    fun SetAlphaLevel(level: String) {
        player.alphaLevel = level
    }

    @JavascriptInterface
    fun GetAlphaLevel(): Int = player.alphaLevel.toIntOrNull() ?: 255

    @JavascriptInterface
    fun SetTransparentColor(color: Int) = Unit

    @JavascriptInterface
    fun GetTransparentColor(): Int = 0

    @JavascriptInterface
    fun SetChromaKey(on: Int, color: Int) = Unit

    @JavascriptInterface fun getChromaKey(): Int = 0

    @JavascriptInterface
    fun SetFlicker(enable: Int, size: Int, count: Int) = Unit

    @JavascriptInterface fun SetDefaultFlicker(value: Int) = Unit
    @JavascriptInterface fun SetUserFlickerControl(enable: Int) = Unit

    @JavascriptInterface
    fun SetVideoControl(enable: Int) = Unit

    @JavascriptInterface
    fun SetVideoState(state: Int) = Unit

    @JavascriptInterface
    fun SetViewport(x: Int, y: Int, width: Int, height: Int) {
        player.setViewport(x, y, width, height)
    }

    @JavascriptInterface
    @JvmOverloads
    fun SetViewportEx(
        x: Int, y: Int, width: Int, height: Int,
        videoWidth: Int = 0, videoHeight: Int = 0,
        mode: Int = 0, crop: Boolean = false, forceMode: Boolean = false,
    ) = SetViewport(x, y, width, height)

    @JavascriptInterface
    fun SetPIG(enabled: Int, x: Int, width: Int, height: Int) {
        player.pictureInPicture = enabled != 0
    }

    @JavascriptInterface
    fun GetPIG(): Boolean = player.pictureInPicture

    @JavascriptInterface
    fun GetPIGState(): Int = if (player.pictureInPicture) 1 else 0

    @JavascriptInterface
    fun SetWinMode(win: Int, mode: Int) = Unit

    @JavascriptInterface
    fun SetWinAlphaLevel(win: Int, level: Int) = Unit

    @JavascriptInterface
    fun GetWinAlphaLevel(win: Int): Int = 255

    @JavascriptInterface
    fun SetTopWin(win: Int): Int = 0

    @JavascriptInterface
    fun SetVideoWindowMode(mode: Int) = Unit

    @JavascriptInterface
    fun ShowVideoImmediately(mode: Int) = 0

    @JavascriptInterface
    fun SetBrightness(value: Int) = Unit

    @JavascriptInterface
    fun GetBrightness(): Int = 255

    @JavascriptInterface
    fun SetSaturation(value: Int) = Unit

    @JavascriptInterface
    fun GetSaturation(): Int = 128

    @JavascriptInterface
    fun SetContrast(value: Int) = Unit

    @JavascriptInterface
    fun GetContrast(): Int = 128

    @JavascriptInterface
    fun SetAutoFrameRate(mode: Int) = Unit

    @JavascriptInterface
    fun SetDisplayMode(mode: String, persist: Boolean) = Unit

    @JavascriptInterface
    fun Set3DConversionMode(mode: Int) = Unit

    @JavascriptInterface
    fun Get3DConversionMode(): Int = 0

    @JavascriptInterface
    fun Set3DtoFullscreenConversionMode(mode: Int) = Unit

    @JavascriptInterface
    fun Get3DtoFullscreenConversionMode(): Int = 0

    // =====================================================================
    // Geraeteidentitaet (Kern der Emulation)
    // =====================================================================

    @JavascriptInterface
    fun GetDeviceModel(): String = host.firmware.model.name

    @JavascriptInterface
    fun GetDeviceModelExt(): String = host.firmware.model.id

    @JavascriptInterface
    fun GetDeviceSerialNumber(): String = cfg.serialNumber

    @JavascriptInterface
    fun GetDeviceVendor(): String = "Infomir"

    @JavascriptInterface
    fun GetDeviceMacAddress(): String = cfg.macAddress

    @JavascriptInterface
    fun GetDeviceVersionHardware(): String = "HW ${host.firmware.model.id}"

    @JavascriptInterface
    fun GetDeviceActiveBank(): String = "A"

    @JavascriptInterface
    fun GetDeviceImageVersion(): String = host.firmware.firmwareVersion

    @JavascriptInterface
    fun GetDeviceImageVersionCurrent(): String = host.firmware.firmwareVersion

    @JavascriptInterface
    fun GetDeviceImageDesc(): String = host.firmware.imageDescription

    /**
     * Stable-UID. Portale benutzen sie, um ein Geraet wiederzuerkennen.
     * Variante 1: aus freier Eingabe ableiten (Hash), damit Portal und
     * Middleware ueber ein Token verknuepft bleiben.
     */
    @JavascriptInterface
    fun GetUID(vararg salt: String): String =
        net.stableUid(salt.filter { it.isNotBlank() })

    @JavascriptInterface
    fun GetNetworkWifiMac(): String = cfg.macAddress

    @JavascriptInterface
    fun GetLanLinkStatus(): Boolean = host.context.runCatching {
        val cm = host.context.getSystemService(android.net.ConnectivityManager::class.java)
        cm?.activeNetwork != null
    }.getOrDefault(false)

    @JavascriptInterface
    fun GetWifiLinkStatus(): Boolean = GetLanLinkStatus()

    @JavascriptInterface
    fun GetNetworkGateways(): String = net.networkGateways()

    @JavascriptInterface
    fun GetNetworkNameServers(): String = net.nameServers()

    @JavascriptInterface
    fun GetPppoeIp(): String = ""

    @JavascriptInterface
    fun GetPppoeLinkStatus(): Boolean = false

    @JavascriptInterface
    fun GetWepKey64ByPassPhrase(pass: String): String = ""

    @JavascriptInterface
    fun GetWepKey128ByPassPhrase(pass: String): String = ""

    @JavascriptInterface
    fun GetWifiGroups(): String = ""

    @JavascriptInterface
    fun GetHDMIConnectionState(): Int = 1

    @JavascriptInterface
    fun ForceHDMItoDVI(mode: Int) = Unit

    @JavascriptInterface
    fun SetInputLang(lang: String) {
        cfg.inputLanguage = lang
    }

    @JavascriptInterface
    fun GetInputLang(): String = cfg.inputLanguage

    @JavascriptInterface
    fun SetUiLang(lang: String) {
        cfg.uiLanguage = lang
    }

    @JavascriptInterface
    fun SetMicVolume(volume: Int) {
        cfg.micVolume = volume
    }

    @JavascriptInterface
    fun GetMicVolume(): Int = cfg.micVolume

    @JavascriptInterface
    fun Rotate(degrees: Int) = Unit

    @JavascriptInterface
    fun SetLedIndicatorLevels(a: Int, b: Int) = Unit

    @JavascriptInterface
    fun setFrontPanel(mode: String, persist: Boolean = false) = Unit

    @JavascriptInterface
    fun SetSyslogLevel(level: Int) {
        cfg.syslogLevel = level
    }

    @JavascriptInterface
    fun GetSystemPaths(): String = cfg.systemPaths()

    @JavascriptInterface
    fun SetNativeStringMode(enabled: Boolean) = Unit

    @JavascriptInterface
    fun SetEventCtrlCallback(num: Int?) = Unit

    @JavascriptInterface
    fun GetLastEvent(): Int = 0

    // =====================================================================
    // Portal / Netzwerk
    // =====================================================================

    @JavascriptInterface
    fun SetWebProxy(host: String, port: Int, exclude: String?, login: String?, pass: String?) =
        net.setProxy(host, port, exclude)

    @JavascriptInterface
    fun ResetWebProxy() = net.resetProxy()

    @JavascriptInterface
    fun CheckPlayerHttpProxy(url: String): Boolean = net.hasProxy()

    @JavascriptInterface
    fun SetCustomHeader(name: String, value: String) = net.setCustomHeader(name, value)

    @JavascriptInterface
    fun SetCheckSSLCertificate(enabled: Int) {
        net.trustAllCertificates = enabled != 0
    }

    @JavascriptInterface
    fun EnableSetCookieFrom(origin: String, enable: Boolean) = Unit

    @JavascriptInterface
    fun SetCustomHeaderIgnoreCase(ignoreCase: Boolean) = Unit

    @JavascriptInterface
    fun EnableMulticastProxy(enable: Boolean) = Unit

    @JavascriptInterface
    fun SetMulticastProxyURL(url: String) = Unit

    @JavascriptInterface
    fun EnableJavaScriptInterrupt(enable: Boolean) = Unit

    @JavascriptInterface
    fun SendEventToPortal(event: String) = net.sendEventToPortal(event)

    @JavascriptInterface
    fun SetEventCallbackExt() = Unit

    @JavascriptInterface
    fun ClearStatistics() = Unit

    @JavascriptInterface
    fun Debug(message: String) {
        host.log.d("[portal] $message")
    }

    @JavascriptInterface
    fun SetSettingsInitAttr(attr: String) = Unit

    @JavascriptInterface
    fun SetScreenSaverInitAttr(attr: String) = Unit

    @JavascriptInterface
    fun SetScreenSaverTime(seconds: Int) {
        cfg.screenSaverTimeoutSec = seconds
    }

    @JavascriptInterface
    fun SetScreenSaverImplUrl(url: String) = Unit

    @JavascriptInterface
    fun SetAutoPowerDownTime(seconds: Int) = Unit

    @JavascriptInterface
    fun SetAutoPowerDownInitAttr(attr: String) = Unit

    @JavascriptInterface
    fun SetWakeUpSources(sources: String) = Unit

    @JavascriptInterface
    fun JSON_GetWakeUpSources(): String = "[]"

    @JavascriptInterface
    fun GETTER_JSON_SupportedWakeUpSources(): String = "[]"

    @JavascriptInterface
    fun GETTER_JSON_SupportedStandByModes(): String = "[]"

    @JavascriptInterface
    fun StandByMode(): Int = 0

    @JavascriptInterface
    fun SetDefaultUpdateUrl(url: String) = Unit

    @JavascriptInterface
    fun GetDefaultUpdateUrl(): String = com.mclone.stbemu.core.stb.FirmwareProfile.DEFAULT_UPDATE_LIST_URL

    @JavascriptInterface
    fun SetupRTSP(port: Int, ttl: Int) = Unit

    // =====================================================================
    // Dateisystem
    // =====================================================================

    @JavascriptInterface
    fun ListDir(path: String): String = host.listDirectory(path, false)

    @JavascriptInterface
    fun ListDir(path: String, withFiles: Boolean): String = host.listDirectory(path, withFiles)

    @JavascriptInterface
    fun SetListFilesExt(ext: String) = Unit

    @JavascriptInterface
    fun IsFileExist(path: String): Boolean = host.fileExists(path)

    @JavascriptInterface
    fun IsFolderExist(path: String): Boolean = host.folderExists(path)

    @JavascriptInterface
    fun RDir(path: String): String = host.readTextFile(path) ?: ""

    @JavascriptInterface
    fun SaveFile(path: String, content: String) {
        host.writeTextFile(path, content)
    }

    @JavascriptInterface
    fun GetStorageInfo(path: String): String = host.storageInfo(path)

    @JavascriptInterface
    fun ResetUserFs() = Unit

    @JavascriptInterface
    fun LoadUserData(name: String): String = host.readTextFile("/home/userdata/$name") ?: ""

    @JavascriptInterface
    fun SaveUserData(name: String, content: String) {
        host.writeTextFile("/home/userdata/$name", content)
    }

    @JavascriptInterface
    fun LoadCASIniFile(path: String) = Unit

    @JavascriptInterface
    fun SetObjectCacheCapacities(a: Int, b: Int, c: Int) = Unit

    // =====================================================================
    // Konfiguration (CFG / Prefs)
    // =====================================================================

    @JavascriptInterface
    fun ReadCFG(): String = cfg.exportAsPortalConfig()

    @JavascriptInterface
    fun WriteCFG(xml: String) {
        cfg.importFromPortalConfig(xml)
    }

    @JavascriptInterface
    fun WritePrefs(xml: String) = WriteCFG(xml)

    @JavascriptInterface
    fun GetEnv(name: String): String? = cfg.environmentValue(name)

    @JavascriptInterface
    fun SetEnv(name: String): Boolean = cfg.setEnvironmentValue(name)

    @JavascriptInterface
    fun SetPlayerOption(key: String, value: String) = cfg.setPlayerOption(key, value)

    @JavascriptInterface
    fun GetVideoInfo(): String = player.videoInfoJson()

    @JavascriptInterface
    fun GetMetadataInfo(): String = player.metadataJson()

    @JavascriptInterface
    fun GetHLSInfo(): String = player.hlsInfoJson()

    @JavascriptInterface
    fun GetBufferLoad(): Int = player.bufferLoadPercent

    @JavascriptInterface
    fun SetBufferSize(video: Int, audio: Int) {
        player.setBufferSize(video, audio)
    }

    @JavascriptInterface
    fun SetInputBufferSize(video: Int, audio: Int) = SetBufferSize(video, audio)

    @JavascriptInterface
    fun GetInputBufferLoad(): Int = player.bufferLoadPercent

    @JavascriptInterface
    fun SetPixmapCacheSize(kb: Int) = Unit

    // =====================================================================
    // Hash / UID-Hilfen
    // =====================================================================

    @JavascriptInterface
    fun getHash(value: String): String = net.hash(value)

    @JavascriptInterface
    fun GetHashVersion1(value: String): String = net.hashVersion1(value)

    @JavascriptInterface
    fun GetHashVersion1(value: String, salt: String): String = net.hashVersion1(value, salt)

    // =====================================================================
    // App-Steuerung
    // =====================================================================

    @JavascriptInterface
    fun startApplication(pkg: String) {
        host.launchApp(pkg)
    }

    @JavascriptInterface
    fun installApplication(pkg: String) = Unit

    @JavascriptInterface
    fun installApplicationByURL(url: String) = Unit

    @JavascriptInterface
    fun isInstalled(pkg: String): Boolean = host.isInstalled(pkg)

    @JavascriptInterface
    fun isPackageInstalled(pkg: String): Boolean = host.isInstalled(pkg)

    @JavascriptInterface
    fun EnableServiceButton(enable: Boolean) = Unit

    @JavascriptInterface
    fun EnableAppButton(enable: Boolean) = Unit

    @JavascriptInterface
    fun EnableTvButton(enable: Boolean) = Unit

    @JavascriptInterface
    fun EnableVKButton(enable: Boolean) = Unit

    @JavascriptInterface
    fun EnableCustomNavigation(enable: Boolean) = Unit

    @JavascriptInterface
    fun EnableSpatialNavigation(enable: Boolean) = Unit

    @JavascriptInterface
    fun ServiceControl(command: String, params: String): String = ""

    @JavascriptInterface
    fun ServiceControl(service: String, command: String, params: String): String = ""

    @JavascriptInterface
    fun GetSmbGroups(): String = "[]"

    @JavascriptInterface
    fun GetSmbServers(group: String): String = "[]"

    @JavascriptInterface
    fun GetSmbShares(server: String): String = "[]"

    @JavascriptInterface
    fun ConfigNetRc(config: String, params: String) = Unit

    @JavascriptInterface
    fun SetNetRcStatus(enabled: Boolean) = Unit

    @JavascriptInterface
    fun GetExtProtocolList(): String = "[]"

    @JavascriptInterface
    fun ExtProtocolCommand(proto: String, command: String, params: String): String = ""

    @JavascriptInterface
    fun SetWebMode(enabled: Boolean, url: String) = Unit

    @JavascriptInterface
    fun LoadURL(url: String) {
        host.loadUrl(url)
    }

    // =====================================================================
    // Teletext
    // =====================================================================

    @JavascriptInterface
    fun SetTeletext(enabled: Int) = Unit

    @JavascriptInterface
    fun GetTeletext(): Int = 0

    @JavascriptInterface
    fun SetTeletextPID(pid: Int) = Unit

    @JavascriptInterface
    fun GetTeletextPID(): Int = 0

    @JavascriptInterface
    fun GetTeletextPIDs(): String = ""

    @JavascriptInterface
    fun TeletextShow(page: String) = Unit

    @JavascriptInterface
    fun TeletextTransparency(level: String) = Unit

    @JavascriptInterface
    fun TeletextCommand(command: String) = Unit

    @JavascriptInterface
    fun GetTeletextLang(): String = ""

    @JavascriptInterface
    fun DefaultTtxSubCharset(): String = "UTF-8"

    @JavascriptInterface
    fun ForceTtxSubCharset(charset: String) = Unit

    @JavascriptInterface
    fun ForceTtxCharset(charset: String, persist: Boolean) = Unit

    @JavascriptInterface
    fun GetTtxForceCharset(): String = cfg.teletextCharset

    @JavascriptInterface
    fun SetDefaultTtxCharset(charset: String) {
        cfg.teletextCharset = charset
    }

    @JavascriptInterface
    fun GetDefaultTtxCharset(): String = cfg.teletextCharset

    @JavascriptInterface
    fun ForceTtxCharsetUltimate(charset: String) = Unit

    @JavascriptInterface
    fun SetPCRModeEnabled(enabled: Int) = Unit

    // ---- Hilfsfunktion ----

    private fun parseTime(value: String): Int? {
        val parts = value.split(':', '.')
        return runCatching {
            when (parts.size) {
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                else -> null
            }
        }.getOrNull()
    }
}
