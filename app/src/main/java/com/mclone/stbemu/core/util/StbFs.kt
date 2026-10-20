package com.mclone.stbemu.core.util

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Abbildung der MAG-Pfadwelt (`/home/web`, `/mnt`, `/storage`, `/var`)
 * auf reale Android-Pfade.
 *
 * Ein Android-Handy hat weder `/home` noch `/var`; Portale erwarten diese
 * Pfade aber in `stb.ListDir()`-Aufrufen und in CFG-Dateien. Deshalb werden
 * sie hier auf App-internen Speicher umgebogen.
 *
 * Rekonstruiert aus `p000.cjq` (Pfadaufloesung) und den Pfad-Konstanten in
 * `assets/mag/default_config.json` ("nfsaddr", "ipaddr", "playlist_url" ...).
 */
object StbFs {

    /** Weltweiter Pfad -> realer Pfad. Reihenfolge = Prioritaet. */
    private val MOUNTS = listOf(
        "/storage" to "extStorage",
        "/mnt" to "extStorage",
        "/home/web" to "workDir/web",
        "/home/userdata" to "workDir/userdata",
        "/var/log" to "workDir/log",
        "/var/tmp" to "workDir/tmp",
        "/tmp" to "cacheDir",
    )

    /** Aufgeloester Pfad oder null, wenn ausserhalb aller Mounts. */
    fun resolve(context: Context, path: String): File? {
        val normalized = path.trim().removePrefix("file://")
        for ((virtual, actual) in MOUNTS) {
            if (normalized == virtual || normalized.startsWith("$virtual/")) {
                val rest = normalized.removePrefix(virtual).trimStart('/')
                return resolveActual(context, actual).let { base ->
                    if (rest.isEmpty()) base else File(base, rest)
                }
            }
        }
        return if (normalized.startsWith("/")) File(normalized) else null
    }

    private fun resolveActual(context: Context, key: String): File = when (key) {
        "extStorage" -> Environment.getExternalStorageDirectory()
            ?: File(context.filesDir, "ext")
        "workDir" -> workDir(context)
        "workDir/web" -> File(workDir(context), "web")
        "workDir/userdata" -> File(workDir(context), "userdata")
        "workDir/log" -> File(workDir(context), "log")
        "workDir/tmp" -> File(workDir(context), "tmp")
        "cacheDir" -> context.cacheDir
        else -> File(context.filesDir, key)
    }.apply { mkdirs() }

    fun workDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "stbemu").apply { mkdirs() }

    fun exists(context: Context, path: String): Boolean =
        resolve(context, path)?.exists() == true

    fun isDirectory(context: Context, path: String): Boolean =
        resolve(context, path)?.isDirectory == true

    /** `stb.ListDir()` liefert ein JSON-Objekt mit `list` und `total`. */
    fun listDir(context: Context, path: String, withFiles: Boolean): String {
        val dir = resolve(context, path)
        val out = JSONArray()
        var total = 0
        dir?.listFiles()?.sortedBy { it.name.lowercase() }?.forEach { file ->
            val isDir = file.isDirectory
            if (!isDir && !withFiles) return@forEach
            total++
            out.put(
                JSONObject().apply {
                    put("name", file.name)
                    put("type", if (isDir) "dir" else "file")
                    put("size", file.length())
                    put("mod_date", file.lastModified() / 1000)
                    put("is_dir", isDir)
                }
            )
        }
        return JSONObject().apply {
            put("list", out)
            put("total", total)
        }.toString()
    }

    fun readText(context: Context, path: String): String? =
        resolve(context, path)?.takeIf { it.isFile }?.readText()

    fun writeText(context: Context, path: String, content: String) {
        val file = resolve(context, path) ?: return
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /** `stb.GetStorageInfo()` - freier und Gesamtplatz in Byte. */
    fun storageInfo(context: Context, path: String): String {
        val dir = resolve(context, path) ?: workDir(context)
        return JSONObject().apply {
            put("path", path)
            put("total", dir.totalSpace)
            put("free", dir.usableSpace)
            put("used", dir.totalSpace - dir.usableSpace)
        }.toString()
    }
}
