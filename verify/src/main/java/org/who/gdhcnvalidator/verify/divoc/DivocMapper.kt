package org.who.gdhcnvalidator.verify.divoc

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates a W3C VC object into FHIR Objects
 */
class DivocMapper: BaseMapper() {
    fun run(payload: W3CVC): Bundle {
        return super.run(
            payload,
            "DIVOCtoDDCC.map"
        )
    }
}