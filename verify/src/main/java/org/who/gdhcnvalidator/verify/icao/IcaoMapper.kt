package org.who.gdhcnvalidator.verify.icao

import org.hl7.fhir.r4.model.Bundle
import org.who.gdhcnvalidator.verify.BaseMapper

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class IcaoMapper: BaseMapper() {
    fun run(iJson: IJson): Bundle {
        return super.run(
            iJson,
            "ICAOtoDDCC.map"
        )
    }
}