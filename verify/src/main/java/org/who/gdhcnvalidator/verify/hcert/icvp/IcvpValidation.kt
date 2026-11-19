package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.StringType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URL
import java.net.URLConnection
import java.io.IOException

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
     * Forces a refresh of the cached product IDs from the source
     * Useful for testing or when the cache needs to be updated
     */
    fun refreshProductIdCache() {
        cachedProductIds = null
    }
    
    /**
     * Loads ICVP Product IDs from the PreQual database
     * Fetches the FHIR CodeSystem from the WHO SMART guidelines and extracts product IDs
     */
    private fun loadIcvpProductIds(): Set<String> {
        return try {
            val url = URL(ICVP_PRODUCT_ID_SYSTEM)
            val connection: URLConnection = url.openConnection()
            connection.setRequestProperty("Accept", "application/fhir+json")
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000 // 10 seconds
            
            val jsonResponse = connection.getInputStream().bufferedReader().use { it.readText() }
            val objectMapper = ObjectMapper()
            val jsonNode: JsonNode = objectMapper.readTree(jsonResponse)
            
            // Extract product IDs from FHIR CodeSystem concept codes
            val productIds = mutableSetOf<String>()
            
            // Check if this is a valid FHIR CodeSystem
            if (jsonNode.has("resourceType") && 
                jsonNode.get("resourceType").asText() == "CodeSystem" &&
                jsonNode.has("concept")) {
                
                val concepts = jsonNode.get("concept")
                if (concepts.isArray) {
                    for (concept in concepts) {
                        if (concept.has("code")) {
                            val code = concept.get("code").asText()
                            if (code.isNotBlank()) {
                                productIds.add(code)
                            }
                        }
                    }
                }
            }
            
            productIds.toSet()
        } catch (e: IOException) {
            // Log the error but don't fail validation entirely
            // In production, you might want to use proper logging here
            System.err.println("Warning: Could not load ICVP Product IDs from ${ICVP_PRODUCT_ID_SYSTEM}: ${e.message}")
            emptySet()
        } catch (e: Exception) {
            // Handle any other parsing or network errors
            System.err.println("Warning: Error parsing ICVP Product IDs: ${e.message}")
            emptySet()
        }
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
        
        // If we successfully loaded product IDs from the source, validate against them
        if (cachedProductIds!!.isNotEmpty()) {
            return cachedProductIds!!.contains(productId)
        }
        
        // Fallback to format validation if source is not available
        // This ensures validation doesn't completely fail if the WHO server is down
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