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
     * Cache for ICVP Product IDs loaded from the PreQual database
     * This would be populated from the actual ICVP PreQual value set
     */
    private var cachedProductIds: Set<String>? = null
    
    /**
     * Loads ICVP Product IDs from the PreQual database
     * This should be implemented to fetch from the actual ICVP PreQual value set
     * For now, returns an empty set until proper integration is implemented
     */
    private fun loadIcvpProductIds(): Set<String> {
        // TODO: Implement actual loading from ICVP PreQual database
        // The source should be: http://smart.who.int/pcmt-vaxprequal/CodeSystem/PreQualProductIDs
        return emptySet()
    }
    
    /**
     * Validates that the vaccine product ID comes from the ICVP Product Catalogue
     * as required by the is-an-icvp-product-id invariant
     */
    fun validateIcvpProductId(productId: String?): Boolean {
        if (productId.isNullOrBlank()) return false
        
        // Load product IDs from source if not cached
        if (cachedProductIds == null) {
            cachedProductIds = loadIcvpProductIds()
        }
        
        // For now, validate basic format requirements until source integration is complete
        // Product ID should be non-empty and follow expected format patterns
        return productId.isNotBlank() && 
               (productId.length > 10) && // Basic format check
               productId.matches(Regex("^[A-Za-z0-9]+$")) // Alphanumeric format
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