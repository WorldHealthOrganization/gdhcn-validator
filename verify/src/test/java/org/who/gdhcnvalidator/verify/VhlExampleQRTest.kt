package org.who.gdhcnvalidator.verify

import org.junit.Assert.*
import org.junit.Test
import org.who.gdhcnvalidator.QRDecoder
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest

class VhlExampleQRTest : BaseTrustRegistryTest() {

    @Test
    fun testRealWorldVhlExamples() {
        val decoder = QRDecoder(registry)
        
        // Test VHL with PIN required
        val vhlWithPin = "vhlink:/eyJ1cmwiOiJodHRwczovL2V4YW1wbGUuY29tL21hbmlmZXN0IiwiZmxhZyI6IlAifQ=="
        val resultWithPin = decoder.decode(vhlWithPin)
        
        assertEquals("Should require PIN", QRDecoder.Status.VHL_REQUIRES_PIN, resultWithPin.status)
        assertNotNull("Should have VHL info", resultWithPin.vhlInfo)
        assertTrue("Should require PIN", resultWithPin.vhlInfo?.requiresPin == true)
        assertEquals("Should extract URL", "https://example.com/manifest", resultWithPin.vhlInfo?.decodedLink?.url)
        assertEquals("Should extract flag", "P", resultWithPin.vhlInfo?.decodedLink?.flag)
        
        // Test VHL without PIN
        val vhlWithoutPin = "vhlink:/eyJ1cmwiOiJodHRwczovL2V4YW1wbGUuY29tL21hbmlmZXN0In0="
        val resultWithoutPin = decoder.decode(vhlWithoutPin)
        
        assertEquals("Should fail to fetch manifest", QRDecoder.Status.VHL_FETCH_ERROR, resultWithoutPin.status)
        assertNotNull("Should have VHL info", resultWithoutPin.vhlInfo)
        assertFalse("Should not require PIN", resultWithoutPin.vhlInfo?.requiresPin == true)
        assertEquals("Should extract URL", "https://example.com/manifest", resultWithoutPin.vhlInfo?.decodedLink?.url)
        
        // Test SHL format - should not be supported per requirements
        val shlExample = "shlink:/eyJ1cmwiOiJodHRwczovL2V4YW1wbGUuY29tL3NobC1tYW5pZmVzdCJ9"
        val shlResult = decoder.decode(shlExample)
        
        assertEquals("Should not support SHL", QRDecoder.Status.NOT_SUPPORTED, shlResult.status)
        assertNull("Should not have VHL info", shlResult.vhlInfo)
        
        // Test VHL with additional parameters
        val vhlFull = "vhlink:/eyJ1cmwiOiJodHRwczovL2V4YW1wbGUuY29tL21hbmlmZXN0IiwiZmxhZyI6IlAiLCJrZXkiOiJhYmNkZWYxMjM0NTYiLCJsYWJlbCI6IlZhY2NpbmF0aW9uIFJlY29yZCIsImV4cCI6MTY5ODc2ODAwMH0="
        val fullResult = decoder.decode(vhlFull)
        
        assertEquals("Should require PIN", QRDecoder.Status.VHL_REQUIRES_PIN, fullResult.status)
        assertNotNull("Should have VHL info", fullResult.vhlInfo)
        assertEquals("Should extract URL", "https://example.com/manifest", fullResult.vhlInfo?.decodedLink?.url)
        assertEquals("Should extract flag", "P", fullResult.vhlInfo?.decodedLink?.flag)
        assertEquals("Should extract key", "abcdef123456", fullResult.vhlInfo?.decodedLink?.key)
        assertEquals("Should extract label", "Vaccination Record", fullResult.vhlInfo?.decodedLink?.label)
        assertEquals("Should extract expiration", 1698768000L, fullResult.vhlInfo?.decodedLink?.exp)
    }
    
    @Test
    fun testInvalidVhlExamples() {
        val decoder = QRDecoder(registry)
        
        // Test invalid base64
        val invalidBase64 = "vhlink:/invalid-base64-data!!!"
        val result1 = decoder.decode(invalidBase64)
        assertEquals("Should detect invalid URI", QRDecoder.Status.VHL_INVALID_URI, result1.status)
        
        // Test invalid JSON
        val invalidJson = "vhlink:/dGhpcyBpcyBub3QganNvbg=="  // "this is not json" in base64
        val result2 = decoder.decode(invalidJson)
        assertEquals("Should detect invalid URI", QRDecoder.Status.VHL_INVALID_URI, result2.status)
        
        // Test non-VHL URI
        val notVhl = "https://example.com/regular-url"
        val result3 = decoder.decode(notVhl)
        assertEquals("Should not support regular URL", QRDecoder.Status.NOT_SUPPORTED, result3.status)
    }
}