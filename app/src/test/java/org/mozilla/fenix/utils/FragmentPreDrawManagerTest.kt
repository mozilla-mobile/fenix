package org.mozilla.fenix.utils

import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.annotation.Config

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class FragmentPreDrawManagerTest {

    private fun doNothing() {  }

    @Test
    fun `execute doOnPreDraw`() {
        runBlockingTest {
            val fragmentMock = mockk<Fragment>(relaxed = true)
            val fragmentPreDrawManager = FragmentPreDrawManager(fragmentMock)

            verify { fragmentMock.postponeEnterTransition() }
            fragmentPreDrawManager.execute { doNothing() }
            verify { fragmentMock.startPostponedEnterTransition() }
        }
    }
}