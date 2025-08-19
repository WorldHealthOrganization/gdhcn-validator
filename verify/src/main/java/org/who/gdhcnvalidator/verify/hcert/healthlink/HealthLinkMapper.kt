package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.who.gdhcnvalidator.verify.BaseMapper
import java.util.*

/**
 * Translates Smart Health Links and Verifiable Health Links (VHL) into FHIR Objects
 */
class HealthLinkMapper: BaseMapper() {
    
    private val vhlVerifier = VhlVerifier()
    
    fun run(link: SmartHealthLinkModel): Bundle {
        return if (link.isVHL()) {
            processVhl(link)
        } else {
            processRegularShl(link)
        }
    }
    
    /**
     * Process Verifiable Health Link (VHL)
     * Creates a placeholder bundle that indicates VHL processing is needed
     */
    private fun processVhl(link: SmartHealthLinkModel): Bundle {
        val bundle = Bundle()
        bundle.id = UUID.randomUUID().toString()
        bundle.type = Bundle.BundleType.DOCUMENT
        
        // Create a composition to indicate this is a VHL
        val composition = Composition()
        composition.id = UUID.randomUUID().toString()
        composition.status = Composition.CompositionStatus.FINAL
        composition.type = createCodeableConcept("VHL", "Verifiable Health Link")
        composition.title = "Verifiable Health Link"
        composition.date = Date()
        
        // Add VHL URI as a section
        val section = Composition.SectionComponent()
        section.title = "VHL URI"
        section.text = createNarrative("VHL URI: ${link.getUri()}")
        composition.section = listOf(section)
        
        // Create placeholder patient
        val patient = Patient()
        patient.id = UUID.randomUUID().toString()
        composition.subject = Reference("Patient/${patient.id}")
        
        // Add to bundle
        bundle.addEntry().setResource(composition)
        bundle.addEntry().setResource(patient)
        
        return bundle
    }
    
    /**
     * Process regular Smart Health Link (SHL)
     * Currently returns placeholder bundle
     */
    private fun processRegularShl(link: SmartHealthLinkModel): Bundle {
        val bundle = Bundle()
        bundle.id = UUID.randomUUID().toString()
        bundle.type = Bundle.BundleType.DOCUMENT
        
        val composition = Composition()
        composition.id = UUID.randomUUID().toString()
        composition.status = Composition.CompositionStatus.FINAL
        composition.type = createCodeableConcept("SHL", "Smart Health Link")
        composition.title = "Smart Health Link"
        composition.date = Date()
        
        // Create placeholder patient
        val patient = Patient()
        patient.id = UUID.randomUUID().toString()
        composition.subject = Reference("Patient/${patient.id}")
        
        bundle.addEntry().setResource(composition)
        bundle.addEntry().setResource(patient)
        
        return bundle
    }
    
    private fun createCodeableConcept(code: String, display: String): org.hl7.fhir.r4.model.CodeableConcept {
        val concept = org.hl7.fhir.r4.model.CodeableConcept()
        val coding = org.hl7.fhir.r4.model.Coding()
        coding.code = code
        coding.display = display
        concept.coding = listOf(coding)
        return concept
    }
    
    private fun createNarrative(text: String): org.hl7.fhir.r4.model.Narrative {
        val narrative = org.hl7.fhir.r4.model.Narrative()
        narrative.status = org.hl7.fhir.r4.model.Narrative.NarrativeStatus.GENERATED
        narrative.div = org.hl7.fhir.utilities.xhtml.XhtmlNode()
        narrative.div.addTag("div").addText(text)
        return narrative
    }
}