package com.mclone.stbemu.core.net

import android.content.Context
import com.mclone.stbemu.core.util.Logger
import com.mclone.stbemu.core.util.Settings

/**
 * Update-Pruefer (Stalker/Ministra-Update-API).
 *
 * Rekonstruiert aus `p000.cuc` (Update-Manager) und `p000.ciu` Bridge.
 * Der Original-Check laeuft bei **jedem** `MainActivity.onCreate`; Intervall
 * (next_check_timestamp) wird geschrieben aber nie gelesen.
 */
class Updater(
    private val context: Context,
    private val settings: Settings,
    private val log: Logger,
) {

    enum class Status { IDLE, CHECKING, DOWNLOADING, INSTALLING, ERROR }

    var status = Status.IDLE
    var statusText = ""
    var progress = 0
    var imageVersion = ""
    var imageDate = ""
    var imageDescription = ""
    var autoUpdateAttr = ""

    fun check(url: String?) {
        status = Status.CHECKING
        statusText = "Pruefe..."
        log.i("Update", "Check ${url ?: "default"}")
        // TODO: echtes HTTP-Get gegen ${url ?: settings.portalServer}/update_list.txt
        // und Parse der Versionsliste.
    }

    fun startUpdate(url: String?) {
        status = Status.DOWNLOADING
        statusText = "Lade herunter..."
        // TODO: Download + Intent.ACTION_VIEW Installation
    }

    fun startAutoUpdate(url: String?, checkOnly: Boolean) {
        if (checkOnly) check(url)
        else startUpdate(url)
    }

    fun emitProgress() { /* no-op, UI zieht sich progress via status */ }

    fun emitStateChanged() { /* no-op */ }
}