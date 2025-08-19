package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.*
import org.junit.Assert.*
import org.junit.Test
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest
import java.util.*

class VhlIntegrationTest : BaseTrustRegistryTest() {

    @Test
    fun testVhlWorkflowWithoutPin() {
        val vhlVerifier = VhlVerifier()
        
        // Create a VHL URI without PIN requirement
        val testData = """{"url":"https://example.com/manifest"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val vhlUri = "vhlink:/$encodedData"
        
        // Test URI decoding
        val decodedLink = vhlVerifier.decodeVhlUri(vhlUri)
        assertNotNull("Should decode VHL URI", decodedLink)
        assertEquals("Should extract URL", "https://example.com/manifest", decodedLink?.url)
        assertFalse("Should not require PIN", vhlVerifier.isPinRequired(decodedLink!!))
    }
    
    @Test
    fun testVhlWorkflowWithPin() {
        val vhlVerifier = VhlVerifier()
        
        // Create a VHL URI with PIN requirement
        val testData = """{"url":"https://example.com/manifest","flag":"P"}"""
        val encodedData = Base64.getUrlEncoder().encodeToString(testData.toByteArray())
        val vhlUri = "vhlink:/$encodedData"
        
        // Test URI decoding
        val decodedLink = vhlVerifier.decodeVhlUri(vhlUri)
        assertNotNull("Should decode VHL URI", decodedLink)
        assertEquals("Should extract URL", "https://example.com/manifest", decodedLink?.url)
        assertEquals("Should extract flag", "P", decodedLink?.flag)
        assertTrue("Should require PIN", vhlVerifier.isPinRequired(decodedLink!!))
    }
    
    @Test
    fun testVhlManifestProcessing() {
        val vhlVerifier = VhlVerifier()
        
        // Create a mock FHIR Bundle representing a VHL manifest
        val manifest = createMockVhlManifest()
        
        // Test file extraction
        val fileList = vhlVerifier.extractFileList(manifest)
        
        assertEquals("Should extract correct number of files", 2, fileList.size)
        
        // Verify PDF file
        val pdfFile = fileList.find { it.type == "PDF" }
        assertNotNull("Should have PDF file", pdfFile)
        assertEquals("Should have correct PDF title", "Vaccination Certificate", pdfFile?.title)
        
        // Verify FHIR IPS file
        val fhirFile = fileList.find { it.type == "FHIR_IPS" }
        assertNotNull("Should have FHIR IPS file", fhirFile)
        assertEquals("Should have correct FHIR title", "FHIR IPS Document", fhirFile?.title)
    }
    
    @Test
    fun testSmartHealthLinkModelVhlDetection() {
        // Test VHL detection
        val vhlModel = SmartHealthLinkModel(StringType("vhlink:/test"))
        assertTrue("Should detect VHL URI", vhlModel.isVHL())
        
        // Test SHL detection  
        val shlModel = SmartHealthLinkModel(StringType("shlink:/test"))
        assertTrue("Should detect SHL URI as VHL", shlModel.isVHL())
        
        // Test non-VHL URI
        val otherModel = SmartHealthLinkModel(StringType("https://example.com"))
        assertFalse("Should not detect regular URI as VHL", otherModel.isVHL())
        
        // Test null URI
        val nullModel = SmartHealthLinkModel(null)
        assertFalse("Should not detect null URI as VHL", nullModel.isVHL())
    }
    
    /**
     * Creates a mock VHL manifest Bundle for testing
     */
    private fun createMockVhlManifest(): Bundle {
        val bundle = Bundle()
        bundle.id = "test-manifest"
        bundle.type = Bundle.BundleType.SEARCHSET
        
        // Create a List resource
        val listResource = ListResource()
        listResource.id = "file-list"
        listResource.status = ListResource.ListStatus.CURRENT
        listResource.mode = ListResource.ListMode.WORKING
        
        // Add PDF document reference
        val pdfDocRef = DocumentReference()
        pdfDocRef.id = "pdf-doc"
        pdfDocRef.description = "Vaccination Certificate"
        val pdfAttachment = Attachment()
        pdfAttachment.url = "https://example.com/cert.pdf"
        pdfAttachment.size = 12345L
        val pdfContent = DocumentReference.DocumentReferenceContentComponent()
        pdfContent.attachment = pdfAttachment
        pdfDocRef.content = listOf(pdfContent)
        
        // Add FHIR IPS Bundle
        val ipsBundle = Bundle()
        ipsBundle.id = "ips-bundle"
        ipsBundle.type = Bundle.BundleType.DOCUMENT
        
        // Add references to List
        val pdfListEntry = ListResource.ListEntryComponent()
        pdfListEntry.item = Reference("DocumentReference/pdf-doc")
        listResource.entry.add(pdfListEntry)
        
        val ipsListEntry = ListResource.ListEntryComponent()
        ipsListEntry.item = Reference("Bundle/ips-bundle")
        listResource.entry.add(ipsListEntry)
        
        // Add resources to Bundle
        bundle.addEntry().setResource(listResource)
        bundle.addEntry().setResource(pdfDocRef)
        bundle.addEntry().setResource(ipsBundle)
        
        return bundle
    }
}