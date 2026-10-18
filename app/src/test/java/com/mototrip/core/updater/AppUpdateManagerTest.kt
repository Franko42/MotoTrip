package com.mototrip.core.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `isNewerVersion detects higher patch, minor and major`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.1.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "2.0.0"))
    }

    @Test
    fun `isNewerVersion handles v and V prefix gracefully`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "v1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.0.0", "v1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("V1.0.0", "V1.0.1"))
    }

    @Test
    fun `isNewerVersion returns false for equal or older versions`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "v1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.1.0", "1.0.9"))
        assertFalse(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun `isNewerVersion handles different segment lengths and pre-releases`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "1.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1-rc1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "1.0.1-rc1"))
    }

    @Test
    fun `isNewerVersion handles blank strings conservatively`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", ""))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "   "))
        assertTrue(AppUpdateManager.isNewerVersion("", "1.0.0"))
    }

    @Test
    fun `parseReleaseResponse parses release with valid APK asset`() {
        val sampleJson = """
            {
              "tag_name": "v1.1.0",
              "name": "MotoTrip v1.1.0",
              "body": "Nuevas funciones de cálculo y estadísticas GPS.",
              "assets": [
                {
                  "name": "mototrip-v1.1.0.apk",
                  "size": 15728640,
                  "browser_download_url": "https://github.com/Franko42/MotoTrip/releases/download/v1.1.0/mototrip-v1.1.0.apk"
                },
                {
                  "name": "source-code.zip",
                  "size": 1024,
                  "browser_download_url": "https://github.com/Franko42/MotoTrip/archive/refs/tags/v1.1.0.zip"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateManager.parseReleaseResponse(sampleJson, currentVersion = "1.0.0")

        assertEquals("1.0.0", info.currentVersion)
        assertEquals("v1.1.0", info.latestVersion)
        assertEquals("MotoTrip v1.1.0", info.releaseTitle)
        assertEquals("Nuevas funciones de cálculo y estadísticas GPS.", info.releaseNotes)
        assertEquals("https://github.com/Franko42/MotoTrip/releases/download/v1.1.0/mototrip-v1.1.0.apk", info.apkDownloadUrl)
        assertEquals("mototrip-v1.1.0.apk", info.apkFileName)
        assertEquals(15728640L, info.apkSizeBytes)
        assertTrue(info.isUpdateAvailable)
    }

    @Test
    fun `parseReleaseResponse ignores update if installed version is same or higher`() {
        val sampleJson = """
            {
              "tag_name": "v1.0.0",
              "name": "MotoTrip v1.0.0",
              "body": "Primera versión.",
              "assets": [
                {
                  "name": "mototrip-v1.0.0.apk",
                  "size": 15000000,
                  "browser_download_url": "https://github.com/Franko42/MotoTrip/releases/download/v1.0.0/mototrip-v1.0.0.apk"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateManager.parseReleaseResponse(sampleJson, currentVersion = "1.0.0")
        assertFalse(info.isUpdateAvailable)
    }

    @Test
    fun `parseReleaseResponse handles release without APK assets`() {
        val sampleJson = """
            {
              "tag_name": "v1.2.0",
              "name": "MotoTrip v1.2.0",
              "body": "Notas sin binario APK adjunto.",
              "assets": [
                {
                  "name": "documentation.pdf",
                  "size": 500000,
                  "browser_download_url": "https://example.com/docs.pdf"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateManager.parseReleaseResponse(sampleJson, currentVersion = "1.0.0")
        assertEquals("v1.2.0", info.latestVersion)
        assertNull(info.apkDownloadUrl)
        assertNull(info.apkFileName)
        assertFalse(info.isUpdateAvailable)
    }
}
