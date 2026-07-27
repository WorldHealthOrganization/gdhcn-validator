package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*

class IcvpVaccineDetails (
    doseNumber: CodeableConcept,
    disease: Coding?,

    vaccineClassification: CodeableConcept,
    vaccineTradeItem: StringType?,
    date: DateTimeType?,

    clinicianName: StringType?,
    issuer: Reference?,

    manufacturerId: Identifier?,
    manufacturer: StringType?,

    batchNo: StringType?,
    validityPeriod: Period?,
): DvcVaccineDetails(doseNumber, disease, vaccineClassification, vaccineTradeItem, date, clinicianName, issuer, manufacturerId, manufacturer, batchNo, validityPeriod) {
    
    /**
     * Validates ICVP-specific constraints including product ID constraints
     */
    override fun validateIcvpConstraints(): List<String> {
        val errors = super.validateIcvpConstraints().toMutableList()
        
        // Validate is-an-icvp-product-id invariant
        val productId = vaccineTradeItem?.value
        if (!IcvpValidation.validateIcvpProductId(productId)) {
            errors.add("Product ID must come from the ICVP vaccines from the PreQual Database")
        }
        
        return errors
    }
}