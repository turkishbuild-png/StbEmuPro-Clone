package com.mclone.stbemu.core.stb

/**
 * Firmware-Profil eines emulierten Receivers.
 *
 * Ein Portal erkennt das emulierte Geraet anhand von User-Agent, Firmware-Version,
 * Build-Datum, Hardware- und Image-Beschreibung. Deshalb liefert jedes Profil
 * diese Werte. Reale Receiver antworten je nach Firmware unterschiedlich auf
 * dieselbe Middleware-API - das ist der Grund fuer die Trennung.
 *
 * Rekonstruiert aus `p000.chj` / `p000.cjp` / `p000.cjr` / `p000.cks`.
 */
interface FirmwareProfile {

    /** Modell, zu dem dieses Firmware-Profil gehoert. */
    val model: StbModel

    // ---- Identifikation -----------------------------------------------------

    /** Firmware-Version, z. B. "0.2.16-234". */
    val firmwareVersion: String

    /** Interne Build-Revision, z. B. "216". */
    val firmwareRevision: String

    /** Build-Datum im MAG-Format, z. B. "Fri Jan 15 15:20:44 EET 2016". */
    val buildDate: String

    /** Bezeichnung des Flash-Images. */
    val imageDescription: String

    /** HTML-Beschreibung des Strecken-Ports; %EMU_DETECTION% wird ersetzt. */
    val imageVersionHtml: String

    // ---- Faehigkeiten -------------------------------------------------------

    /** Unterstuetzte Bildschirmaufloesungen, z. B. ["1280x720", "1920x1080"]. */
    val screenResolutions: List<String>

    /** Standardaufloesung. */
    val defaultScreenResolution: String

    /** Unterstuetzte Video-Modi (PAL/NTSC, interlaced/progressiv, Hz). */
    val videoModes: List<String>

    /** Standard-Videomodus. */
    val defaultVideoMode: String

    /** true, wenn der Receiver einen CAS-/Descrambling-Stack besitzt. */
    val supportsCas: Boolean

    /** true, wenn TimeShift (PVR) unterstuetzt wird. */
    val supportsTimeShift: Boolean

    /** true, wenn der PVR-Aufnahmepuffer behalten wird (Power-Off-Restart). */
    val supportsPvrPersistence: Boolean

    /** Maximale Anzahl paralleler Player-Instanzen. */
    val maxPlayers: Int

    // ---- Ports --------------------------------------------------------------

    /**
     * Namen der JavaScript-Objekte, die in den WebView injiziert werden.
     * Reihenfolge irrelevant, Name = Schluessel des JS-Globals.
     *
     * Rekonstruiert aus `p000.cjp.mo6955a(...)`.
     */
    val jsApiClasses: Map<String, Class<*>>

    // ---- User-Agent ---------------------------------------------------------

    /**
     * Baut den HTTP-User-Agent.
     *
     * Das Original ersetzt in `p000.cul.m8512g()` den Platzhalter
     * `%EMU_DETECTION%` durch "; Emulator/1.1.9", sofern der Nutzer die
     * Emulator-Erkennung nicht abgeschaltet hat.
     */
    fun userAgent(detectEmulator: Boolean): String =
        imageVersionHtml.replace(EMU_DETECTION_PLACEHOLDER, if (detectEmulator) "; Emulator/1.1.9" else "")

    companion object {
        const val EMU_DETECTION_PLACEHOLDER = "%EMU_DETECTION%"

        /** Standard-Update-Liste des Herstellers (aus `p000.cjp.f9160d`). */
        const val DEFAULT_UPDATE_LIST_URL = "http://mag.infomir.com.ua/250/update_list.txt"

        /** Standard-Update-Endpunkt (aus `p000.cjp.f9161e`). */
        const val DEFAULT_IMAGE_UPDATE_URL = "http://mag.infomir.com.ua/250/imageupdate"

        /** Basis-Firmware der 250er-Serie (aus `p000.cjp.f9164h`). */
        const val BASE_HARDWARE_REVISION = 250
    }
}
