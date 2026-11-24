package com.lavish.admob.analytics

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MixpanelTrackerTest {

    @Test
    fun testAnonymousDistinctIdGeneratedInitially() {
        val settings = MapSettings()
        val tracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        val distinctId = tracker.getDistinctId()
        assertTrue(distinctId.isNotBlank(), "Distinct ID should not be blank")
        assertTrue(distinctId.startsWith("anon_"), "Anonymous ID should have 'anon_' prefix: $distinctId")
    }

    @Test
    fun testIdentifyUpdatesDistinctId() {
        val settings = MapSettings()
        val tracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        val initialAnonId = tracker.getDistinctId()
        tracker.identify("publisher@domain.com", mapOf("\$name" to "Publisher User"))

        assertEquals("publisher@domain.com", tracker.getDistinctId())
        assertNotEquals(initialAnonId, tracker.getDistinctId())
    }

    @Test
    fun testResetClearsIdentifiedUserAndCreatesNewAnonymousId() {
        val settings = MapSettings()
        val tracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        tracker.identify("user@test.com")
        assertEquals("user@test.com", tracker.getDistinctId())

        tracker.reset()
        val postResetId = tracker.getDistinctId()
        assertNotEquals("user@test.com", postResetId)
        assertTrue(postResetId.startsWith("anon_"))
    }

    @Test
    fun testConfiguredTokenIsSecurelyLoaded() {
        // Verify MixpanelConfig holds the configured token securely without throwing
        assertTrue(MixpanelConfig.isConfigured, "MixpanelConfig should have a token configured")
        assertTrue(MixpanelConfig.TOKEN.isNotBlank(), "Configured token should not be blank")
    }

    @Test
    fun testNavigationEventHelpersDoNotThrow() {
        val settings = MapSettings()
        val tracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        tracker.trackScreenViewed(
            screenName = "Today Dashboard",
            previousScreen = "Splash Screen",
            tabName = "TODAY",
            additionalData = mapOf("custom_metric" to 42)
        )

        tracker.trackTabSwitched(
            fromTab = "TODAY",
            toTab = "APPS",
            source = "bottom_dock"
        )

        tracker.trackModalOpened(
            modalName = "App Detail Modal",
            triggerSource = "today_recent_apps",
            details = mapOf("app_name" to "SolarAd Pro")
        )

        tracker.trackModalDismissed(
            modalName = "App Detail Modal",
            dismissMethod = "system_back"
        )

        tracker.trackBackPressed(
            currentScreen = "App Detail: SolarAd Pro",
            actionTaken = "dismiss_app_detail"
        )
    }

    @Test
    fun testUserLifecycleHelpers() {
        val settings = MapSettings()
        val tracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        tracker.trackLoginInitiated("Google OAuth")
        tracker.trackLoginSucceeded(
            method = "Google OAuth",
            userId = "test_user_id",
            email = "user@test.com",
            publisherId = "pub-123456789",
            userName = "Test User"
        )

        assertEquals("user@test.com", tracker.getDistinctId())

        tracker.trackUserLoggedOut("user@test.com")
        assertTrue(tracker.getDistinctId().startsWith("anon_"))
    }

    @Test
    fun testRevenueSnapshotTracking() {
        val settings = MapSettings()
        val tracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        tracker.trackRevenueSnapshot(
            todayRevenue = 145.20,
            yesterdayRevenue = 132.80,
            dailyGoal = 150.0,
            dailyGoalProgress = 0.968f,
            deltaAmount = 12.40,
            deltaPercentage = 9.33,
            ecpm = 4.25,
            impressions = 34100,
            clicks = 680,
            currency = "USD",
            appsCount = 3,
            topEarningAppName = "SolarAd Pro"
        )
    }

    @Test
    fun testDesktopPlatformDisablesMixpanelTracking() {
        // Under JVM/Desktop test runtime, isMixpanelSupported() should evaluate to false
        assertFalse(isMixpanelSupported(), "Desktop JVM platform should not support Mixpanel tracking")

        val settings = MapSettings()
        val defaultTracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings
        )

        assertFalse(defaultTracker.isSupportedOnPlatform, "Default tracker on Desktop should have isSupportedOnPlatform = false")
        assertFalse(defaultTracker.isEnabled, "Default tracker on Desktop should have isEnabled = false")

        // Tracking on disabled platform safely skips execution without crashing or throwing
        defaultTracker.track("Test Event", mapOf("foo" to "bar"))
        defaultTracker.setUserProfile(mapOf("test_prop" to 123))
        defaultTracker.flush()
    }

    @Test
    fun testExplicitlyEnabledTrackerIsActive() {
        val settings = MapSettings()
        val enabledTracker = MixpanelTracker(
            token = "test_token_123",
            settings = settings,
            isPlatformEnabled = true
        )

        assertTrue(enabledTracker.isSupportedOnPlatform, "Tracker with isPlatformEnabled=true should be supported")
        assertTrue(enabledTracker.isEnabled, "Tracker with isPlatformEnabled=true and non-blank token should be enabled")
    }

    @Test
    fun testTrackerWithBlankTokenIsNotEnabledEvenIfPlatformSupported() {
        val settings = MapSettings()
        val trackerNoToken = MixpanelTracker(
            token = "",
            settings = settings,
            isPlatformEnabled = true
        )

        assertTrue(trackerNoToken.isSupportedOnPlatform)
        assertFalse(trackerNoToken.isEnabled, "Tracker with blank token should not be enabled")
    }
}
