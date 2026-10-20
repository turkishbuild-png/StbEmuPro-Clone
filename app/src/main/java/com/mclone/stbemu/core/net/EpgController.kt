package com.mclone.stbemu.core.net

import com.mclone.stbemu.core.util.Logger

/**
 * EPG-Manager fuer `epgManager` Bridge.
 *
 * Das Original (`p000.cia`) faehrt Scanner (`StartChannelScan`), haelt
 * `channels`/`events` und liefert JSON an das Portal.
 */
class EpgController(
    private val api: StalkerApi,
    private val log: Logger,
) {

    fun brief(args: String): String = "{}"
    fun schedule(args: String): String = "[]"
    fun schedule(args: String, day: Int): String = "[]"
    fun channelList(args: String): String = "[]"
    fun supportedScanTypes(): String = "[\"AUTO\"]"
    fun currentScanTypes(args: String): String = "[]"
}