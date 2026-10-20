package com.mclone.stbemu.core.net

import android.content.Context
import com.mclone.stbemu.core.util.Logger

/**
 * Download-Manager fuer Updates und Portal-Medien.
 *
 * Rekonstruiert aus `p000.cij` (`stbDownloadManager` Bridge) und
 * `com.mvas.stbemu.services.TaskSchedulerService` (Jobs persistieren).
 */
class DownloadManager(
    private val context: Context,
    private val log: Logger,
) {

    private val jobs = mutableMapOf<Long, DownloadJob>()
    private var nextId = 0L

    fun addJob(url: String, name: String?): Boolean = runCatching {
        val id = nextId++
        jobs[id] = DownloadJob(id, url, name ?: url.substringAfterLast('/'), System.currentTimeMillis())
        true
    }.getOrElse {
        log.w("addJob failed", it)
        false
    }

    fun addMeasureJob(url: String): Boolean = addJob(url, "measure_$url")

    fun startJob(id: Long): Boolean = jobs[id]?.let { it.status = 1; true } ?: false

    fun stopJob(id: Long): Boolean = jobs[id]?.let { it.status = 2; true } ?: false

    fun deleteJob(id: Long, alsoFile: Boolean): Boolean = jobs.remove(id)?.let {
        if (alsoFile) File(it.destPath).delete()
        true
    } ?: false

    fun adjustPriority(id: Long, up: Boolean): Boolean = jobs[id]?.let {
        it.priority += if (up) -1 else 1
        true
    } ?: false

    fun playDownloaded(id: Long) = jobs[id]?.let { log.i("DL", "play ${it.destPath}") }

    fun queueInfoJson(): String = org.json.JSONArray(jobs.values.map { it.toJson() }).toString()

    fun queueInfoJson(filter: String): String = org.json.JSONArray(
        jobs.values.filter { it.name.contains(filter) }.map { it.toJson() }
    ).toString()

    fun measureInfoJson(): String = org.json.JSONArray(
        jobs.values.filter { it.name.startsWith("measure_") }.map { it.toJson() }
    ).toString()

    fun restoreJobs(path: String) = runCatching {
        val txt = File(path).readText()
        val arr = org.json.JSONArray(txt)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val job = DownloadJob(
                id = o.getLong("id"),
                url = o.getString("url"),
                name = o.getString("name"),
                started = o.getLong("started"),
            ).apply {
                destPath = o.optString("dest", "")
                status = o.optInt("status", 0)
                progress = o.optLong("progress", 0L)
                total = o.optLong("total", -1L)
                priority = o.optInt("priority", 0)
            }
            jobs[job.id] = job
            nextId = maxOf(nextId, job.id + 1)
        }
    }.onFailure { log.w("restoreJobs failed", it) }

    data class DownloadJob(
        val id: Long,
        val url: String,
        val name: String,
        val started: Long,
    ) {
        var destPath = ""
        var status = 0        // 0=wartend, 1=laeuft, 2=gestoppt, 3=fertig, 4=fehler
        var progress = 0L
        var total = -1L
        var priority = 0

        fun toJson() = org.json.JSONObject().apply {
            put("id", id)
            put("url", url)
            put("name", name)
            put("started", started)
            put("dest", destPath)
            put("status", status)
            put("progress", progress)
            put("total", total)
            put("priority", priority)
        }
    }
}