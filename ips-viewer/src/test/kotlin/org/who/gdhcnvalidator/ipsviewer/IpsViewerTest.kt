package org.who.gdhcnvalidator.ipsviewer

import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

/**
 * Tests for the IPS Viewer library
 */
class IpsViewerTest {

    @Test
    fun `test IPS viewer creation and basic functionality`() {
        val ipsViewer = IpsViewer()
        assertNotNull(ipsViewer)
    }

    @Test
    fun `test basic patient parsing`() {
        val bundle = createTestIpsBundle()
        val ipsViewer = IpsViewer()
        
        assertTrue(ipsViewer.isValidIpsBundle(bundle))
        
        val processedIps = ipsViewer.processIpsBundle(bundle)
        
        assertEquals("John Doe", processedIps.patient.displayName)
        assertEquals("male", processedIps.patient.gender)
        assertNotNull(processedIps.patient.birthDate)
        
        val formattedText = ipsViewer.formatIpsAsText(bundle)
        assertTrue(formattedText.contains("John Doe"))
        assertTrue(formattedText.contains("Patient Information"))
    }

    @Test
    fun `test clinical alerts detection`() {
        val bundle = createTestIpsBundleWithAllergies()
        val ipsViewer = IpsViewer()
        
        val alerts = ipsViewer.getClinicalAlerts(bundle)
        
        assertTrue(alerts.isNotEmpty())
        assertTrue(alerts.any { it.level == AlertLevel.HIGH })
        assertTrue(alerts.any { it.title.contains("Critical Allergies") })
    }

    @Test
    fun `test metadata extraction`() {
        val bundle = createTestIpsBundle()
        val ipsViewer = IpsViewer()
        
        val metadata = ipsViewer.getIpsMetadata(bundle)
        
        assertEquals("International Patient Summary", metadata.documentType)
        assertEquals("R4", metadata.fhirVersion)
        assertTrue(metadata.totalResources > 0)
    }

    @Test
    fun `test safe IPS viewer`() {
        val safeViewer = SafeIpsViewer()
        val bundle = createTestIpsBundle()
        
        val result = safeViewer.processIpsBundle(bundle)
        assertTrue(result is IpsResult.Success)
        
        when (result) {
            is IpsResult.Success -> {
                assertEquals("John Doe", result.data.patient.displayName)
            }
            is IpsResult.Error -> fail("Expected success, got error: ${result.message}")
        }
    }

    @Test
    fun `test invalid bundle handling`() {
        val invalidBundle = Bundle() // Empty bundle
        val safeViewer = SafeIpsViewer()
        
        assertFalse(safeViewer.isValidIpsBundle(invalidBundle))
        
        val result = safeViewer.processIpsBundle(invalidBundle)
        assertTrue(result is IpsResult.Error)
    }

    private fun createTestIpsBundle(): Bundle {
        val bundle = Bundle()
        bundle.type = Bundle.BundleType.DOCUMENT
        
        // Add patient
        val patient = Patient()
        patient.id = "patient-1"
        patient.addName().addGiven("John").family = "Doe"
        patient.gender = Enumerations.AdministrativeGender.MALE
        patient.birthDate = java.util.Date(90, 0, 1) // Jan 1, 1990
        
        val patientEntry = Bundle.BundleEntryComponent()
        patientEntry.resource = patient
        bundle.addEntry(patientEntry)
        
        // Add composition
        val composition = Composition()
        composition.id = "composition-1"
        composition.status = Composition.CompositionStatus.FINAL
        composition.type = CodeableConcept()
        composition.type.addCoding().code = "60591-5"
        composition.title = "International Patient Summary"
        composition.subject = Reference("Patient/patient-1")
        
        val compositionEntry = Bundle.BundleEntryComponent()
        compositionEntry.resource = composition
        bundle.addEntry(compositionEntry)
        
        return bundle
    }

    private fun createTestIpsBundleWithAllergies(): Bundle {
        val bundle = createTestIpsBundle()
        
        // Add critical allergy
        val allergy = AllergyIntolerance()
        allergy.id = "allergy-1"
        allergy.clinicalStatus = CodeableConcept()
        allergy.clinicalStatus.addCoding().code = "active"
        allergy.criticality = AllergyIntolerance.AllergyIntoleranceCriticality.HIGH
        allergy.code = CodeableConcept()
        allergy.code.text = "Penicillin"
        allergy.patient = Reference("Patient/patient-1")
        
        val allergyEntry = Bundle.BundleEntryComponent()
        allergyEntry.resource = allergy
        bundle.addEntry(allergyEntry)
        
        return bundle
    }
}