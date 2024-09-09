package org.who.gdhcnvalidator.verify.hcert.ddcc

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class DdccMapper: BaseMapper() {
    fun run(ddcc: DdccCoreDataSet): Bundle {
        return super.run(
            ddcc,
            "DDCCtoDDCC.map"
        )
    }
}