package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Narrative
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates legacy Health Links found in CBOR payloads into FHIR Objects
 * Note: Modern VHL processing (vhlink:/ URIs) is handled directly in VhlVerifier
 */
class HealthLinkMapper: BaseMapper() {
    
    fun run(link: SmartHealthLinkModel): Bundle {
        // Create a basic bundle for legacy healthLink fields found in CBOR payloads
        // Modern VHL URIs (vhlink:/) are processed directly in QRDecoder/VhlVerifier
        val bundle = Bundle().apply {
            type = Bundle.BundleType.DOCUMENT
            id = "healthlink-legacy"
        }
        
        // Create a composition indicating this is a legacy health link
        val composition = Composition().apply {
            id = "composition-healthlink"
            status = Composition.CompositionStatus.FINAL
            type = org.hl7.fhir.r4.model.CodeableConcept().apply {
                text = "Legacy Health Link"
            }
            title = "Health Link Reference"
            
            // Add the link URI as narrative text
            text = Narrative().apply {
                status = Narrative.NarrativeStatus.GENERATED
                div = org.hl7.fhir.utilities.xhtml.XhtmlNode().apply {
                    addTag("div")
                    addTag("p").addText("Legacy health link found in certificate: ${link.getUri() ?: "Unknown URI"}")
                    addTag("p").addText("Modern VHL processing requires vhlink:/ URI format.")
                }
            }
        }
        
        bundle.addEntry().apply {
            resource = composition
        }
        
        return bundle
    }
}