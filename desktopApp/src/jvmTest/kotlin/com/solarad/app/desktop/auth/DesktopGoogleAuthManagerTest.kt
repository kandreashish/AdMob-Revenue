package com.solarad.app.desktop.auth

import com.russhwolf.settings.MapSettings
import com.solarad.app.data.AdMobRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopGoogleAuthManagerTest {

    @Test
    fun testGenerateCodeVerifier_validLengthAndChars() {
        val testRepo = AdMobRepository(settings = MapSettings())
        val authManager = DesktopGoogleAuthManager(testRepo)

        val verifier1 = authManager.generateCodeVerifier()
        val verifier2 = authManager.generateCodeVerifier()

        assertTrue(verifier1.length >= 43)
        assertTrue(verifier1.length <= 128)
        assertFalse(verifier1 == verifier2, "Subsequent verifiers should be distinct random strings")

        // URL-safe charset check (A-Z, a-z, 0-9, -, _, ~, .) without base64 padding '='
        assertFalse(verifier1.contains('='), "Verifier should not contain padding '='")
        assertFalse(verifier1.contains('+'), "Verifier should be URL-safe (no '+')")
        assertFalse(verifier1.contains('/'), "Verifier should be URL-safe (no '/')")
    }

    @Test
    fun testGenerateCodeChallenge_deterministicForSameVerifier() {
        val testRepo = AdMobRepository(settings = MapSettings())
        val authManager = DesktopGoogleAuthManager(testRepo)

        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val challenge1 = authManager.generateCodeChallenge(verifier)
        val challenge2 = authManager.generateCodeChallenge(verifier)

        assertEquals(challenge1, challenge2, "Code challenge must be deterministic for identical verifier")
        assertFalse(challenge1.contains('='), "Challenge should not contain padding '='")
        assertFalse(challenge1.contains('+'), "Challenge should be URL-safe (no '+')")
        assertFalse(challenge1.contains('/'), "Challenge should be URL-safe (no '/')")
    }

    @Test
    fun testBuildAuthUrl_containsAllRequiredParameters() {
        val testRepo = AdMobRepository(settings = MapSettings())
        val authManager = DesktopGoogleAuthManager(testRepo)

        val clientId = "test-client-id.apps.googleusercontent.com"
        val redirectUri = "http://127.0.0.1:8585/oauth2callback"
        val codeChallenge = "test_challenge_hash"
        val state = "test_state_123"

        val url = authManager.buildAuthUrl(clientId, redirectUri, codeChallenge, state)

        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"))
        assertTrue(url.contains("client_id=test-client-id.apps.googleusercontent.com"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("code_challenge=test_challenge_hash"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=test_state_123"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("prompt=consent"))
        assertTrue(url.contains("admob.readonly"))
        assertTrue(url.contains("admob.report"))
    }

    @Test
    fun testRefreshTokenLifecycleInRepository() {
        val settings = MapSettings()
        val testRepo = AdMobRepository(settings = settings)
        val authManager = DesktopGoogleAuthManager(testRepo)

        assertNull(testRepo.getStoredRefreshToken())

        testRepo.saveRefreshToken("1//04test_refresh_token_abc")
        assertEquals("1//04test_refresh_token_abc", testRepo.getStoredRefreshToken())

        authManager.signOut()
        assertNull(testRepo.getStoredRefreshToken())
    }
}
