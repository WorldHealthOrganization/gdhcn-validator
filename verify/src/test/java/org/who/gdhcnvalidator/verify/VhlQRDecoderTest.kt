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
    fun testInvalidShlUri() {
        val decoder = QRDecoder(registry)
        
        // SHL URIs should not be processed as VHL
        val testData = """{"url":"https://example.com/manifest"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val shlUri = "shlink:/$encodedData"
        
        val result = decoder.decode(shlUri)
        
        // SHL should not be supported
        assertEquals("Should not support SHL", QRDecoder.Status.NOT_SUPPORTED, result.status)
        assertNull("Should not have VHL info", result.vhlInfo)
    }
    
    @Test
    fun testInvalidVhlUri() {
        val decoder = QRDecoder(registry)
        
        val result = decoder.decode("vhlink:/invalid-base64-data")
        
        assertEquals("Should detect invalid VHL URI", QRDecoder.Status.VHL_INVALID_URI, result.status)
        assertNull("Should not have VHL info", result.vhlInfo)
    }
}