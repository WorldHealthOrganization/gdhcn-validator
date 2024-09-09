package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.HCertDVC

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class DvcMapper: BaseMapper() {
    fun run(ddcc: HCertDVC): Bundle {
        return super.run(
            ddcc,
            "DvcToDDCC.map"
        )
    }
}