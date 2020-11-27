package org.mozilla.fenix.perf

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import io.mockk.every
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import java.io.File

@RunWith(FenixRobolectricTestRunner::class)
class PerformanceInflaterTest {

    private lateinit var perfInflater : MockInflater

    private val layoutNotToTest = setOf(
        "fragment_browser",
        "fragment_add_on_internal_settings"
    )

    private val layoutWithMerge = setOf(
        "mozac_ui_tabcounter_layout",
        "tabstray_multiselect_items",
        "tab_preview",
        "tracking_protection_category"
    )

    @Before
    fun setup() {
        InflationCounter.inflationCount.set(0)

        perfInflater = MockInflater(LayoutInflater.from(testContext), testContext)
    }

    @Test
    fun `WHEN we inflate a view, the inflation counter should increase`() {
        assertEquals(0, InflationCounter.inflationCount.get())
        perfInflater.inflate(R.layout.fragment_home, null, false)
        assertEquals(1, InflationCounter.inflationCount.get())
    }

    @Test
    fun `WHEN inflating one of our resource file, the inflater should not crash`(){
        val fileList = File("./src/main/res/layout").listFiles()
        if(fileList != null){
            for(file in fileList){
                val layoutName = file.name.split(".")[0]
                val layoutId = testContext.resources.getIdentifier(
                    layoutName,
                    "layout",
                    testContext.packageName
                )
                if(layoutId != -1 && !layoutNotToTest.contains(layoutName)){
                    perfInflater.inflate(layoutId, FrameLayout(testContext), true)
                }
            }
        }
    }
}

private class MockInflater(
    inflater: LayoutInflater,
    context: Context
) : PerformanceInflater(
    inflater,
    context
) {

    override fun onCreateView(name: String?, attrs: AttributeSet?): View? {
        if(name!!.contains("fragment")) {
            return FrameLayout(testContext)
        }
        return super.onCreateView(name, attrs)
    }
}