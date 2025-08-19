package org.who.gdhcnvalidator.verify.shc

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class ShcMapper: BaseMapper() {
    fun run(payload: JWTPayload): Bundle {
        return super.run(
            payload,
            "SHCtoDDCC.map"
        )
    }
}