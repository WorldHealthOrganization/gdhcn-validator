package org.who.gdhcnvalidator.verify

import org.junit.Assert.*
import org.junit.Test
import org.who.gdhcnvalidator.QRDecoder
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest
import java.util.*

class VhlQRDecoderTest : BaseTrustRegistryTest() {

    @Test
    fun testVhlUriDetection() {
        val decoder = QRDecoder(registry)
        
        // Create a sample VHL URI
        val testData = """{"url":"https://example.com/manifest","flag":"P"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val vhlUri = "vhlink:/$encodedData"
        
        val result = decoder.decode(vhlUri)
        
        assertEquals("Should detect VHL and require PIN", QRDecoder.Status.VHL_REQUIRES_PIN, result.status)
        assertNotNull("Should have VHL info", result.vhlInfo)
        assertTrue("Should require PIN", result.vhlInfo?.requiresPin == true)
        assertEquals("Should preserve original QR", vhlUri, result.qr)
    }
    
    @Test
    fun testShlUriDetection() {
        val decoder = QRDecoder(registry)
        
        // Create a sample SHL URI (no PIN required)
        val testData = """{"url":"https://example.com/manifest"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val shlUri = "shlink:/$encodedData"
        
        val result = decoder.decode(shlUri)
        
        // Since we can't actually fetch the manifest, expect fetch error
        assertEquals("Should detect SHL but fail to fetch", QRDecoder.Status.VHL_FETCH_ERROR, result.status)
        assertNotNull("Should have VHL info", result.vhlInfo)
        assertFalse("Should not require PIN", result.vhlInfo?.requiresPin == true)
        assertEquals("Should preserve original QR", shlUri, result.qr)
    }
    
    @Test
    fun testInvalidVhlUri() {
        val decoder = QRDecoder(registry)
        
        val result = decoder.decode("vhlink:/invalid-base64-data")
        
        assertEquals("Should detect invalid VHL URI", QRDecoder.Status.VHL_INVALID_URI, result.status)
        assertNull("Should not have VHL info", result.vhlInfo)
    }
}