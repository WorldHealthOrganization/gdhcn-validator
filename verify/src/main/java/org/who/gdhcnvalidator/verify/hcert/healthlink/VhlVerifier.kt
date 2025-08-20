package org.who.gdhcnvalidator.verify.hcert.healthlink

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ListResource
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import kotlin.text.Charsets.UTF_8

/**
 * Verifies and processes Verifiable Health Links (VHL) according to the VHL specification
 * https://build.fhir.org/ig/IHE/ITI.VHL/branches/master/volume-1.html
 */
class VhlVerifier {
    
    data class VhlManifestRequest(
        val url: String,
        val pin: String? = null
    )
    
    data class VhlDecodedLink(
        val url: String,
        val flag: String? = null,
        val key: String? = null,
        val label: String? = null,
        val exp: Long? = null
    )
    
    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()
    
    /**
     * Decodes a VHL URI (vhlink:/) to extract the manifest URL
     */
    fun decodeVhlUri(uri: String): VhlDecodedLink? {
        return try {
            val payload = when {
                uri.startsWith("vhlink:/") -> uri.substring(8)
                else -> return null
            }
            
            val decodedBytes = Base64.getUrlDecoder().decode(payload)
            val decodedJson = String(decodedBytes, UTF_8)
            mapper.readValue(decodedJson, VhlDecodedLink::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Fetches the VHL manifest from the decoded URL
     * Returns a FHIR SearchSet Bundle containing List resources and included items
     */
    fun fetchManifest(request: VhlManifestRequest): Bundle? {
        return try {
            val url = if (request.pin != null) {
                // Add PIN parameter if provided
                val separator = if (request.url.contains("?")) "&" else "?"
                "${request.url}${separator}recipient=${URLDecoder.decode(request.pin, "UTF-8")}"
            } else {
                request.url
            }
            
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/fhir+json")
                .GET()
                .build()
            
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                mapper.readValue(response.body(), Bundle::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if a PIN is required for accessing the manifest
     * This is typically indicated by specific flags in the decoded link or 401 response
     */
    fun isPinRequired(decodedLink: VhlDecodedLink): Boolean {
        // Check if flag indicates PIN is required (P flag for password/PIN required)
        return decodedLink.flag?.contains("P") == true
    }
    
    /**
     * Extracts file information from the VHL manifest
     * Supports both current VHL manifest format and deprecated SHL manifest format
     * Returns list of file metadata for display to user
     */
    fun extractFileList(manifest: Bundle): List<VhlFileInfo> {
        val files = mutableListOf<VhlFileInfo>()
        
        // Check if this is a current VHL manifest (FHIR SearchSet Bundle with List resources)
        if (manifest.type == Bundle.BundleType.SEARCHSET) {
            extractFromCurrentVhlManifest(manifest, files)
        } else {
            // Try deprecated SHL manifest format (files array in Bundle entries)
            extractFromDeprecatedShlManifest(manifest, files)
        }
        
        return files
    }
    
    /**
     * Extract files from current VHL manifest format (FHIR SearchSet Bundle with List resources)
     */
    private fun extractFromCurrentVhlManifest(manifest: Bundle, files: MutableList<VhlFileInfo>) {
        // Process List resources and their included items
        manifest.entry?.forEach { entry ->
            when (val resource = entry.resource) {
                is ListResource -> {
                    resource.entry?.forEach { listEntry ->
                        val reference = listEntry.item?.reference
                        if (reference != null) {
                            // Find the referenced resource in the Bundle
                            val referencedResource = manifest.entry?.find { 
                                it.resource?.id == reference.substringAfter("/")
                            }?.resource
                            
                            if (referencedResource != null) {
                                files.add(extractFileInfo(referencedResource))
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Extract files from deprecated SHL manifest format
     * Based on https://build.fhir.org/ig/HL7/smart-health-cards-and-links/StructureDefinition-ShlManifest.html
     */
    private fun extractFromDeprecatedShlManifest(manifest: Bundle, files: MutableList<VhlFileInfo>) {
        // In deprecated format, files are directly in Bundle entries
        manifest.entry?.forEach { entry ->
            val resource = entry.resource
            if (resource != null) {
                files.add(extractFileInfo(resource))
            }
        }
    }
    
    private fun extractFileInfo(resource: org.hl7.fhir.r4.model.Resource): VhlFileInfo {
        // Extract file information based on resource type
        return when (resource.resourceType.name) {
            "DocumentReference" -> {
                val docRef = resource as org.hl7.fhir.r4.model.DocumentReference
                VhlFileInfo(
                    id = docRef.id ?: "unknown",
                    type = "PDF", // Assume PDF for DocumentReference
                    title = docRef.description ?: "Document",
                    url = docRef.content?.firstOrNull()?.attachment?.url,
                    size = docRef.content?.firstOrNull()?.attachment?.size
                )
            }
            "Bundle" -> {
                val bundle = resource as Bundle
                VhlFileInfo(
                    id = bundle.id ?: "unknown",
                    type = "FHIR_IPS", // Assume IPS for Bundle
                    title = "FHIR IPS Document",
                    content = bundle // Store the bundle for direct processing
                )
            }
            else -> {
                VhlFileInfo(
                    id = resource.id ?: "unknown",
                    type = "UNKNOWN",
                    title = "Unknown Resource Type: ${resource.resourceType.name}"
                )
            }
        }
    }
}

/**
 * Represents a file available in a VHL manifest
 */
data class VhlFileInfo(
    val id: String,
    val type: String, // "PDF", "FHIR_IPS", "UNKNOWN"
    val title: String,
    val url: String? = null,
    val size: Long? = null,
    val content: Any? = null // For direct FHIR content
)