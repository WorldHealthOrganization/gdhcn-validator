package org.who.gdhcnvalidator.verify.hcert.healthlink

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Model for Health Link references found in HCERT claim 5.
 * Supports VHL (vhlink:/) and SMART Health Link (shlink://) URI formats.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
open class SmartHealthLinkModel (
    @JsonProperty("u")
    val u: String?
) {
    /**
     * Checks if this is a VHL (Verifiable Health Link) based on URI prefix
     */
    fun isVHL(): Boolean {
        val uri = u ?: return false
        return uri.startsWith("vhlink:/") || uri.startsWith("shlink:/")
    }

    /**
     * Gets the raw URI value
     */
    fun getUri(): String? = u
}
