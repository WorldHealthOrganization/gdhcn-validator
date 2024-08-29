package org.who.gdhcnvalidator.verify.hcert.who

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.WHOLogicalModel

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class WhoMapper: BaseMapper() {
    fun run(who: WHOLogicalModel): Bundle {
        return super.run(
            who,
            "WHOtoDDCC.map"
        )
    }
}