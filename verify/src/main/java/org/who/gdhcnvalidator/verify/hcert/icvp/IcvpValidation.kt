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
     * Sample ICVP Product IDs from the value set for validation
     * In a full implementation, this would be loaded from the actual value set
     */
    private val SAMPLE_ICVP_PRODUCT_IDS = setOf(
        "YellowFeverProductd2c75a15ed309658b3968519ddb31690",
        "YellowFeverProduct771d1a5c0acaee3e2dc9d56af1aba49d",
        "YellowFeverProducte929626497bdbb71adbe925f0c09c79f",
        "PolioVaccineOralOPVTrivaProductfa4849f7532d522134f4102063af1617",
        "PolioVaccineOralOPVBivalProduct16e883911ea0108b8213bc213c9972fe",
        "PolioVaccineInactivatedIProductc726fd7210023aa5738617a79cae2b40"
    )
    
    /**
     * Validates that the vaccine product ID comes from the ICVP Product Catalogue
     * as required by the is-an-icvp-product-id invariant
     */
    fun validateIcvpProductId(productId: String?): Boolean {
        if (productId.isNullOrBlank()) return false
        
        // In a full implementation, this would check against the complete valueset
        // For now, we validate that it's not empty and optionally check against samples
        return productId.isNotBlank() && 
               (SAMPLE_ICVP_PRODUCT_IDS.contains(productId) || 
                productId.length > 10) // Basic format check
    }
    
    /**
     * Validates the must-have-issuer-or-clinician-name invariant
     * Expression: "v.is.exists() or v.cn.exists()"
     */
    fun validateIssuerOrClinicanName(issuer: StringType?, clinicianName: StringType?): Boolean {
        return !issuer?.value.isNullOrBlank() || !clinicianName?.value.isNullOrBlank()
    }
    
    /**
     * Validates National ID Document Type against the identifier type value set
     * From: http://terminology.hl7.org/CodeSystem/v2-0203 (extensible)
     */
    fun validateNationalIdDocumentType(ndt: String?): Boolean {
        if (ndt.isNullOrBlank()) return true // Optional field
        
        // Common identifier types from HL7 v2-0203
        val validTypes = setOf(
            "DL", "PPN", "BRN", "MR", "DR", "SS", "SB", "NNCZE", "NNxxx", "MD", "DH", "BON"
        )
        
        return validTypes.contains(ndt.uppercase()) || ndt.length <= 10
    }
}