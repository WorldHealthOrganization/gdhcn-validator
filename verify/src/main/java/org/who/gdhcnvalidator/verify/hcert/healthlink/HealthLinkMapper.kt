package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates Verifiable Health Links (VHL) into FHIR Objects
 * Note: VHL processing is handled directly in VhlVerifier, not through this mapper
 */
class HealthLinkMapper: BaseMapper() {
    
    fun run(link: SmartHealthLinkModel): Bundle {
        // VHL processing is handled directly in QRDecoder and VhlVerifier
        // This mapper is maintained for compatibility but not used in VHL workflow
        throw UnsupportedOperationException("VHL processing is handled directly in VhlVerifier")
    }
}