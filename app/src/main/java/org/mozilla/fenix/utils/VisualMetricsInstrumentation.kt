package org.mozilla.fenix.utils

import android.app.Instrumentation
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.*
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BaseBrowserFragment
import org.mozilla.fenix.browser.BrowserFragment
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.search.SearchFragment

@ExperimentalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class VisualMetricsInstrumentation(private val naviageToUrl: String): FragmentManager.FragmentLifecycleCallbacks(){

    private  var runnableEvent : Thread

    init{
        runnableEvent = Thread {
            try{
                val inst = Instrumentation()
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
            }catch(e: InterruptedException){
                Log.e("error dispatching key", "test failed")
            }
        }
    }


    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        super.onFragmentCreated(fm, f, savedInstanceState)
        if (f is SearchFragment){
            runBlocking {
                f.activity!!.window.decorView.foreground = ColorDrawable(Color.GREEN)
            }
        } else if (f is BaseBrowserFragment){
            runBlocking {
                f.activity!!.window.decorView.foreground = ColorDrawable(Color.RED)
            }
        }
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        super.onFragmentResumed(fm, f)
        if(f is HomeFragment){
            f.view!!.post{
                f.activity!!.window.decorView.foreground = null
                f.view!!.postOnAnimation{
                    f.view!!.toolbar_wrapper.callOnClick()
                }
            }
        } else if(f is SearchFragment) {
            f.view!!.post{
                f.activity!!.window.decorView.foreground = null
            }
            f.view!!.postOnAnimationDelayed({
                f.view!!.post{
                    (f.view!! as ViewGroup).children.iterator().forEach {
                        if(it.resources.getResourceName(it.id).contains("toolbar_wrapper")){
                            val toolBar = ((it as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(0) as BrowserToolbar
                            val url = toolBar.findViewById<InlineAutocompleteEditText>(
                                R.id.mozac_browser_toolbar_edit_url_view
                            )
                            url.setText(naviageToUrl)
                            runnableEvent.start()
                        }
                    }
                }
            }, 1000)
        } else if(f is BrowserFragment){
            f.view!!.post{
                f.activity!!.window.decorView.foreground = null
            }
        }
    }
}