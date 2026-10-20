package com.mclone.stbemu.core.pvr

import android.content.Context
import com.mclone.stbemu.core.util.Logger

/**
 * PVR / TimeShift / Catch-up-Controller.
 *
 * Das Original (`com.mvas.stbemu.pvr.MagPvrService` + `p000.cif`) liefert
 * einen lokalen HTTP-Loopback-Server auf `127.0.0.1:PORT/timeshift/...`
 * fuer den Timeshift-Stream und eine JSON-API fuer den Aufnahmescheduler.
 */
class PvrController(
    private val context: Context,
    private val log: Logger,
) {

    var timeshiftFolder: String = ""
        set(value) {
            field = value
            File(value).mkdirs()
        }

    var timeshiftMaxSec: Int = 3600
        set(value) { field = value.coerceIn(60, 86400) }

    var timeshiftSliding: Boolean = false

    var maxRecordings: Int = 10

    private val tasks = mutableMapOf<Long, PvrTask>()

    /** Interne ID fuer naechsten Task. */
    private var nextId = System.currentTimeMillis()

    /** Format, den der Portal-JS-Client erwartet: `CreateTask(url, name, fromMs, toMs)` */
    fun createTask(url: String, name: String, fromMs: Long, toMs: Long): String {
        val id = nextId++
        tasks[id] = PvrTask(id, url, name, fromMs, toMs)
        return id.toString()
    }

    /** Alternative Signatur: `CreateTask(url, name, fromStr, toStr)` ISO-8601. */
    fun createTask(url: String, name: String, from: String, to: String): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
        return createTask(
            url, name,
            runCatching { fmt.parse(from)?.time ?: System.currentTimeMillis() }.getOrDefault(0L),
            runCatching { fmt.parse(to)?.time ?: System.currentTimeMillis() }.getOrDefault(0L),
        )
    }

    fun allTasksJson(): String = org.json.JSONArray(tasks.values.map { it.toJson() }).toString()

    fun tasksByIdsJson(ids: String): String = org.json.JSONArray(
        ids.split(',').mapNotNull { it.toLongOrNull()?.let { tasks[it] }?.toJson() }
    ).toString()

    fun taskByIdJson(id: String): String = tasks[id.toLongOrNull() ?: 0]?.toJson().toString() ?: "{}"

    fun removeTask(id: String, alsoDelete: Boolean): Boolean {
        val task = tasks.remove(id.toLongOrNull() ?: 0)
        if (alsoDelete && task != null) {
            File(task.url).delete()
        }
        return task != null
    }

    fun changeEndTime(id: String, end: String): Boolean {
        val task = tasks[id.toLongOrNull() ?: 0] ?: return false
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
        runCatching { fmt.parse(end)?.time?.let { task.toMs = it } }
        return true
    }

    fun timeshiftOn(enable: Boolean) {
        log.i("PVR", "timeshift ${if (enable) "on" else "off"} (folder=$timeshiftFolder, max=$timeshiftMaxSec sec)")
    }

    fun timeshiftEnter(offsetSec: Int) {
        log.i("PVR", "timeshift enter offset=$offsetSec sec")
    }

    fun timeshiftExit() {
        log.i("PVR", "timeshift exit")
    }

    fun timeshiftExitAndSave(a: String?, b: String?, sec: Int): Boolean {
        log.i("PVR", "timeshift exit&save a=$a b=$b sec=$sec")
        return true
    }

    data class PvrTask(
        val id: Long,
        val url: String,
        val name: String,
        var fromMs: Long,
        var toMs: Long,
    ) {
        fun toJson(): org.json.JSONObject = org.json.JSONObject().apply {
            put("id", id)
            put("url", url)
            put("name", name)
            put("start", fromMs)
            put("end", toMs)
            put("status", 0)
        }
    }
}