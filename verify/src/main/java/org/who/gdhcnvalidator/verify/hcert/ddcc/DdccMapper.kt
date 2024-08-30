package org.who.gdhcnvalidator.verify.hcert.ddcc

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.DdccLogicalModel

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class DdccMapper: BaseMapper() {
    fun run(ddcc: DdccLogicalModel): Bundle {
        return super.run(
            ddcc,
            "DDCCtoDDCC.map"
        )
    }
}