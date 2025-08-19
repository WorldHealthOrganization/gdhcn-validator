package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class HealthLinkMapper: BaseMapper() {
    fun run(link: SmartHealthLinkModel): Bundle {
        // TODO: how do we parse this?
        return Bundle()
    }
}