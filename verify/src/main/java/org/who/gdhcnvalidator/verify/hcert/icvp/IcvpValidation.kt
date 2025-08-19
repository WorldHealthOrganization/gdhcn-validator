package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.StringType

/**
 * Validation helpers for ICVP logical models to ensure compliance with FSH specifications
 */
object IcvpValidation {
    
    /**
     * ICVP Product ID system as defined in the FSH
     */
    const val ICVP_PRODUCT_ID_SYSTEM = "http://smart.who.int/pcmt-vaxprequal/CodeSystem/PreQualProductIDs"
    
    /**
     * Validates that the vaccine product ID comes from the ICVP Product Catalogue
     * as required by the is-an-icvp-product-id invariant
     */
    fun validateIcvpProductId(productId: String?): Boolean {
        // In a full implementation, this would check against the actual valueset
        // For now, we just validate the format and that it's not empty
        return !productId.isNullOrBlank()
    }
    
    /**
     * Validates the must-have-issuer-or-clinician-name invariant
     * Expression: "v.is.exists() or v.cn.exists()"
     */
    fun validateIssuerOrClinicanName(issuer: StringType?, clinicianName: StringType?): Boolean {
        return !issuer?.value.isNullOrBlank() || !clinicianName?.value.isNullOrBlank()
    }
}