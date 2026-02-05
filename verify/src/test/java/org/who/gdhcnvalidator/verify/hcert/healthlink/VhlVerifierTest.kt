package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.junit.Assert.*
import org.junit.Test
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest
import java.util.*

class VhlVerifierTest : BaseTrustRegistryTest() {

    @Test
    fun testDecodeVhlUri() {
        val vhlVerifier = VhlVerifier()
        
        // Create a sample VHL URI with base64 encoded JSON
        val testData = """{"url":"https://example.com/manifest","flag":"P"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val vhlUri = "vhlink:/$encodedData"
        
        val result = vhlVerifier.decodeVhlUri(vhlUri)
        
        assertNotNull("Should decode VHL URI successfully", result)
        assertEquals("Should extract correct URL", "https://example.com/manifest", result?.url)
        assertEquals("Should extract correct flag", "P", result?.flag)
    }
    
    @Test
    fun testDecodeShlUri() {
        val vhlVerifier = VhlVerifier()
        
        // SHL URIs should not be processed by VHL verifier
        val testData = """{"url":"https://example.com/shl-manifest"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val shlUri = "shlink:/$encodedData"
        
        val result = vhlVerifier.decodeVhlUri(shlUri)
        
        assertNull("Should not decode SHL URI (VHL verifier only processes vhlink:/ URIs)", result)
    }
    
    @Test
    fun testInvalidUri() {
        val vhlVerifier = VhlVerifier()
        
        val result = vhlVerifier.decodeVhlUri("invalid-uri")
        
        assertNull("Should return null for invalid URI", result)
    }
    
    @Test
    fun testIsPinRequired() {
        val vhlVerifier = VhlVerifier()
        
        val linkWithPin = VhlVerifier.VhlDecodedLink(
            url = "https://example.com/manifest",
            flag = "P"
        )
        
        val linkWithoutPin = VhlVerifier.VhlDecodedLink(
            url = "https://example.com/manifest",
            flag = null
        )
        
        assertTrue("Should require PIN when flag contains P", vhlVerifier.isPinRequired(linkWithPin))
        assertFalse("Should not require PIN when flag is null", vhlVerifier.isPinRequired(linkWithoutPin))
    }
}