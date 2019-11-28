package org.mozilla.fenix.components

import android.view.View
import android.view.ViewStub
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.lang.ref.WeakReference

class InflationAwareFeatureTest {
    @Test
    fun `stub inflates if no feature or view exists`() {
        val stub: ViewStub = mock()
        val feature: InflationAwareFeature = spy(TestableInflationAwareFeature(stub))

        feature.launch()

        verify(stub).setOnInflateListener(any())
        verify(stub).inflate()
    }

    @Test
    fun `stub immediately launches if the feature is available`() {
        val stub: ViewStub = mock()
        val feature: InflationAwareFeature = spy(TestableInflationAwareFeature(stub))

        feature.feature = mock()
        feature.view = WeakReference(mock())

        feature.launch()

        verify(stub, never()).setOnInflateListener(any())
        verify(stub, never()).inflate()
        verify(feature).onLaunch(any(), any())
    }

    @Test
    fun `feature calls stop if created`() {
        val stub: ViewStub = mock()
        val inflationFeature: InflationAwareFeature = spy(TestableInflationAwareFeature(stub))
        val innerFeature: LifecycleAwareFeature = mock()

        inflationFeature.stop()

        verify(innerFeature, never()).stop()

        inflationFeature.feature = innerFeature

        inflationFeature.stop()

        verify(innerFeature).stop()
    }

    @Test
    fun `start should be delegated to the inner feature`() {
        val inflationFeature: InflationAwareFeature = spy(TestableInflationAwareFeature(mock()))
        val innerFeature: LifecycleAwareFeature = mock()
        inflationFeature.feature = innerFeature

        inflationFeature.start()

        verify(innerFeature).start()
    }

    @Test
    fun `if feature has implemented UserInteractionHandler invoke it`() {
        val stub: ViewStub = mock()
        val inflationFeature: InflationAwareFeature = spy(TestableInflationAwareFeature(stub))
        val innerFeature: LifecycleAwareFeature = mock()
        val userInteractionHandlerFeature = object : LifecycleAwareFeature, UserInteractionHandler {
            override fun onBackPressed() = true

            override fun start() {}

            override fun stop() {}
        }

        assert(!inflationFeature.onBackPressed())

        inflationFeature.feature = innerFeature

        assert(!inflationFeature.onBackPressed())

        inflationFeature.feature = userInteractionHandlerFeature

        assert(inflationFeature.onBackPressed())
    }
}

class TestableInflationAwareFeature(stub: ViewStub) : InflationAwareFeature(stub) {
    override fun onViewInflated(view: View): LifecycleAwareFeature {
        return mock()
    }

    override fun onLaunch(view: View, feature: LifecycleAwareFeature) {
    }
}
