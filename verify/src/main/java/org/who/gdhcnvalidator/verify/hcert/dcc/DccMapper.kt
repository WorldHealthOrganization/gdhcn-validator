package org.who.gdhcnvalidator.verify.hcert.dcc

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.CWT

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class DccMapper: BaseMapper() {
    fun run(cwt: CWT): Bundle {
        return super.run(
            cwt,
            "EUDCCtoDDCC.map"
        )
    }
}