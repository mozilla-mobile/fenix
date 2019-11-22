package org.mozilla.fenix.tabtray

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

import org.mozilla.fenix.R
import org.mozilla.fenix.ext.logDebug


class TabTrayFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tab_tray, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.tab_tray_title)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        logDebug("dw-testing", "onCreateOptionsMenu!")
        inflater.inflate(R.menu.tab_tray_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_menu_item -> {
                logDebug("dw-testing", "SELECT!")
                true
            }
            R.id.share_menu_item -> {
                logDebug("dw-testing", "SHARE!")
                true
            }
            R.id.close_menu_item -> {
                logDebug("dw-testing", "CLOSE!")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}
