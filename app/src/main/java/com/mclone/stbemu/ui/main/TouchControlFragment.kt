package com.mclone.stbemu.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mclone.stbemu.R

/**
 * Touch-Steuerung (Swipe-Gesten -> Fernbedienungstasten).
 *
 * Rekonstruiert aus `com.mvas.stbemu.gui.fragments.TouchControlFragment`.
 * Erlaubt Navigation durch Wischen (hoch/runter/links/rechts), Tap = OK,
 * Double-Tap = Menu, Long-Press = Back.
 */
class TouchControlFragment : Fragment(R.layout.fragment_touch_control) {

    private var lastTap = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val touchArea = view.findViewById<View>(R.id.touch_area)
        touchArea.setOnTouchListener { _, event ->
            onTouchEvent(event)
            true
        }
    }

    private fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()
                    if (now - lastTap < 300) {
                        // Double-Tap = Menu
                        sendKey(82)
                        lastTap = 0
                        return@let true
                    }
                    lastTap = now
                }
                MotionEvent.ACTION_UP -> {
                    val dx = e.x - e.rawX
                    val dy = e.y - e.rawY
                    if (abs(dx) > abs(dy)) {
                        if (dx > 50) sendKey(22)   // Rechts
                        else if (dx < -50) sendKey(21) // Links
                    } else {
                        if (dy > 50) sendKey(20)    // Runter
                        else if (dy < -50) sendKey(19) // Hoch
                    }
                }
            }
        }
        return true
    }

    private fun sendKey(keyCode: Int) {
        (activity as? MainActivity)?.onKeyDown(keyCode, null)
    }
}