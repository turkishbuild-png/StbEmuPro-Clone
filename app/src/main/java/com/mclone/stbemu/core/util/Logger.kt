package com.mclone.stbemu.core.util

import android.util.Log

/** Duenne Huelle um android.util.Log, damit der Kern testbar bleibt. */
interface Logger {
    fun d(msg: String)
    fun i(tag: String, msg: String)
    fun w(msg: String, t: Throwable?)
    fun e(msg: String, t: Throwable?)
}

object AndroidLogger : Logger {
    private const val TAG = "StbEmu"

    override fun d(msg: String) {
        if (Settings.DEBUG) Log.d(TAG, msg)
    }

    override fun i(tag: String, msg: String) {
        if (Settings.DEBUG) Log.i(TAG, "[$tag] $msg")
    }

    override fun w(msg: String, t: Throwable?) {
        if (Settings.DEBUG) Log.w(TAG, msg, t)
    }

    override fun e(msg: String, t: Throwable?) {
        Log.e(TAG, msg, t)
    }
}
