package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.StringType
import org.who.gdhcnvalidator.verify.BaseModel

/**
 * Model for Verifiable Health Links (VHL)
 * Supports only VHL (vhlink:/) URI format
 */
open class SmartHealthLinkModel (
    val u: StringType?
): BaseModel() {
    
    /**
     * Checks if this is a VHL (Verifiable Health Link) based on URI prefix
     */
    fun isVHL(): Boolean {
        val uri = u?.value ?: return false
        return uri.startsWith("vhlink:/")
    }
    
    /**
     * Gets the raw URI value
     */
    fun getUri(): String? = u?.value
}