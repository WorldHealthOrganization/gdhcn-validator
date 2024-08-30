package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.DdccLogicalModel

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class IcvpMapper: BaseMapper() {
    fun run(ddcc: DdccLogicalModel): Bundle {
        return super.run(
            ddcc,
            "ICVPtoDDCC.map"
        )
    }
}