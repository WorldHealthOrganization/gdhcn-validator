package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.StringType
import org.who.gdhcnvalidator.verify.BaseModel

/**
 * Model for Smart Health Links and Verifiable Health Links (VHL)
 * Supports both SHL (shlink:/) and VHL (vhlink:/) URI formats
 */
open class SmartHealthLinkModel (
    val u: StringType?
): BaseModel() {
    
    /**
     * Checks if this is a VHL (Verifiable Health Link) based on URI prefix
     */
    fun isVHL(): Boolean {
        val uri = u?.value ?: return false
        return uri.startsWith("vhlink:/") || uri.startsWith("shlink:/")
    }
    
    /**
     * Gets the raw URI value
     */
    fun getUri(): String? = u?.value
}