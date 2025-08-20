package org.who.gdhcnvalidator.ipsviewer

import org.hl7.fhir.r4.model.Bundle

/**
 * Main facade for IPS viewing functionality
 * Provides a simple interface for parsing and displaying FHIR IPS documents
 */
class IpsViewer {
    
    private val parser = IpsParser()
    private val processor = IpsProcessor()
    private val formatter = IpsFormatter()

    /**
     * Parse and process a FHIR Bundle as an IPS document
     * 
     * @param bundle FHIR Bundle containing IPS data
     * @return Processed IPS document ready for display
     * @throws IllegalArgumentException if bundle is not a valid IPS document
     */
    fun processIpsBundle(bundle: Bundle): ProcessedIpsDocument {
        val ipsDocument = parser.parseIpsBundle(bundle)
        return processor.processIpsDocument(ipsDocument)
    }

    /**
     * Create a comprehensive text representation of an IPS document
     * 
     * @param bundle FHIR Bundle containing IPS data
     * @return Formatted text suitable for display
     */
    fun formatIpsAsText(bundle: Bundle): String {
        val processedIps = processIpsBundle(bundle)
        return formatter.formatIpsDocument(processedIps)
    }

    /**
     * Create a brief summary of an IPS document
     * 
     * @param bundle FHIR Bundle containing IPS data
     * @return Brief formatted text summary
     */
    fun formatIpsBrief(bundle: Bundle): String {
        val processedIps = processIpsBundle(bundle)
        return formatter.formatBriefSummary(processedIps)
    }

    /**
     * Extract just the patient information from an IPS bundle
     * 
     * @param bundle FHIR Bundle containing IPS data
     * @return Processed patient information
     */
    fun extractPatientInfo(bundle: Bundle): ProcessedPatientInfo {
        val processedIps = processIpsBundle(bundle)
        return processedIps.patient
    }

    /**
     * Check if a FHIR Bundle contains the minimum required resources for an IPS
     * 
     * @param bundle FHIR Bundle to validate
     * @return true if bundle appears to be a valid IPS document
     */
    fun isValidIpsBundle(bundle: Bundle): Boolean {
        return try {
            val entries = bundle.entry ?: return false
            
            // Must have a Patient resource
            val hasPatient = entries.any { it.resource is org.hl7.fhir.r4.model.Patient }
            
            // Should have a Composition resource for a complete IPS
            val hasComposition = entries.any { it.resource is org.hl7.fhir.r4.model.Composition }
            
            // Basic validation - must have patient, composition is recommended but not required
            hasPatient
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get clinical alerts from an IPS document
     * 
     * @param bundle FHIR Bundle containing IPS data
     * @return List of clinical alerts
     */
    fun getClinicalAlerts(bundle: Bundle): List<ClinicalAlert> {
        val processedIps = processIpsBundle(bundle)
        return processedIps.alerts
    }

    /**
     * Get document metadata from an IPS bundle
     * 
     * @param bundle FHIR Bundle containing IPS data
     * @return IPS metadata
     */
    fun getIpsMetadata(bundle: Bundle): IpsMetadata {
        val processedIps = processIpsBundle(bundle)
        return processedIps.metadata
    }
}

/**
 * Result of IPS processing operations
 */
sealed class IpsResult<out T> {
    data class Success<T>(val data: T) : IpsResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : IpsResult<Nothing>()
}

/**
 * Safe version of IpsViewer that returns results instead of throwing exceptions
 */
class SafeIpsViewer {
    
    private val ipsViewer = IpsViewer()

    /**
     * Safely process an IPS bundle, returning a result instead of throwing exceptions
     */
    fun processIpsBundle(bundle: Bundle): IpsResult<ProcessedIpsDocument> {
        return try {
            val result = ipsViewer.processIpsBundle(bundle)
            IpsResult.Success(result)
        } catch (e: Exception) {
            IpsResult.Error("Failed to process IPS bundle: ${e.message}", e)
        }
    }

    /**
     * Safely format an IPS bundle as text
     */
    fun formatIpsAsText(bundle: Bundle): IpsResult<String> {
        return try {
            val result = ipsViewer.formatIpsAsText(bundle)
            IpsResult.Success(result)
        } catch (e: Exception) {
            IpsResult.Error("Failed to format IPS bundle: ${e.message}", e)
        }
    }

    /**
     * Safely check if a bundle is a valid IPS
     */
    fun isValidIpsBundle(bundle: Bundle): Boolean {
        return ipsViewer.isValidIpsBundle(bundle)
    }
}