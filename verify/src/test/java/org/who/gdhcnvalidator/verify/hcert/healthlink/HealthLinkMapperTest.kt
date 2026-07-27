package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert.*
import org.junit.Test

class HealthLinkMapperTest {

    @Test
    fun testHealthLinkMapperDoesNotThrowException() {
        val mapper = HealthLinkMapper()
        val model = SmartHealthLinkModel(StringType("vhlink:/test"))
        
        // Should not throw UnsupportedOperationException anymore
        val result = mapper.run(model)
        
        assertNotNull("Should return a Bundle", result)
        assertEquals("Should be document bundle", Bundle.BundleType.DOCUMENT, result.type)
        assertEquals("Should have correct ID", "healthlink-legacy", result.id)
        
        // Should have a composition entry
        val compositionEntry = result.entry?.find { it.resource is Composition }
        assertNotNull("Should have composition entry", compositionEntry)
        
        val composition = compositionEntry?.resource as Composition
        assertEquals("Should have correct status", Composition.CompositionStatus.FINAL, composition.status)
        assertEquals("Should have correct title", "Health Link Reference", composition.title)
    }
    
    @Test
    fun testHealthLinkMapperWithNullUri() {
        val mapper = HealthLinkMapper()
        val model = SmartHealthLinkModel(null)
        
        // Should handle null URI gracefully
        val result = mapper.run(model)
        
        assertNotNull("Should return a Bundle", result)
        assertTrue("Should contain 'Unknown URI' in narrative", 
            result.entry?.any { entry ->
                val composition = entry.resource as? Composition
                composition?.text?.div?.allText?.contains("Unknown URI") == true
            } ?: false)
    }
    
    @Test
    fun testHealthLinkMapperWithShlUri() {
        val mapper = HealthLinkMapper()
        val model = SmartHealthLinkModel(StringType("shlink:/example"))
        
        // Should handle SHL URIs gracefully (legacy case)
        val result = mapper.run(model)
        
        assertNotNull("Should return a Bundle", result)
        assertTrue("Should contain the SHL URI in narrative", 
            result.entry?.any { entry ->
                val composition = entry.resource as? Composition
                composition?.text?.div?.allText?.contains("shlink:/example") == true
            } ?: false)
    }
}