package com.mclone.stbemu.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mclone.stbemu.R

/**
 * On-Screen-Buttons (virtuelle Fernbedienung unten/seitlich).
 *
 * Rekonstruiert aus `com.mvas.stbemu.gui.fragments.ScreenButtonsFragment`.
 * Zeigt Pfeiltasten, OK, Zurück, Menue, Farbtasten, Ziffernblock.
 */
class ScreenButtonsFragment : Fragment(R.layout.fragment_screen_buttons) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Buttons an Player/JS weiterleiten
        view.findViewById<View>(R.id.btn_up).setOnClickListener { sendKey(19) }
        view.findViewById<View>(R.id.btn_down).setOnClickListener { sendKey(20) }
        view.findViewById<View>(R.id.btn_left).setOnClickListener { sendKey(21) }
        view.findViewById<View>(R.id.btn_right).setOnClickListener { sendKey(22) }
        view.findViewById<View>(R.id.btn_ok).setOnClickListener { sendKey(23) }
        view.findViewById<View>(R.id.btn_back).setOnClickListener { sendKey(4) }
        view.findViewById<View>(R.id.btn_menu).setOnClickListener { sendKey(82) }
        view.findViewById<View>(R.id.btn_red).setOnClickListener { sendKey(183) }
        view.findViewById<View>(R.id.btn_green).setOnClickListener { sendKey(184) }
        view.findViewById<View>(R.id.btn_yellow).setOnClickListener { sendKey(185) }
        view.findViewById<View>(R.id.btn_blue).setOnClickListener { sendKey(186) }
        view.findViewById<View>(R.id.btn_exit).setOnClickListener { sendKey(111) }
        view.findViewById<View>(R.id.btn_info).setOnClickListener { sendKey(165) }
        view.findViewById<View>(R.id.btn_epg).setOnClickListener { sendKey(164) }
        view.findViewById<View>(R.id.btn_fav).setOnClickListener { sendKey(166) }
        view.findViewById<View>(R.id.btn_rec).setOnClickListener { sendKey(167) }
    }

    private fun sendKey(keyCode: Int) {
        (activity as? MainActivity)?.let {
            it.onKeyDown(keyCode, null)
        }
    }
}