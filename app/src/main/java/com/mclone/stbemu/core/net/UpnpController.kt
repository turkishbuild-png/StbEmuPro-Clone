package com.mclone.stbemu.core.net

import android.content.Context
import com.mclone.stbemu.core.util.Logger

/**
 * UPnP/DLNA-Controller (Cling-basiert im Original).
 *
 * Das Original bindet `org.fourthline.cling.android.AndroidUpnpServiceImpl`
 * als Service ein und registriert `org.fourthline.cling` im Manifest.
 * Der Klon stellt nur die Bridge-API bereit; Cling ist optional.
 */
class UpnpController(
    private val context: Context,
    private val log: Logger,
) {

    var filterName = ""
    var filterType = 0
    var contextName = ""

    fun startServer(name: String) {
        log.i("UPnP", "startServer($name)")
        // TODO: Cling-Integration
    }

    fun openPage(page: Int) {
        log.i("UPnP", "openPage($page)")
    }
}