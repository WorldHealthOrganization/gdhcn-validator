package org.who.gdhcnvalidator.verify.hcert.healthlink

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.DirectDecrypter
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Resource
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

/**
 * Verifies and processes Verifiable Health Links (VHL) according to the VHL specification
 * https://build.fhir.org/ig/IHE/ITI.VHL/branches/master/volume-1.html
 *
 * Manifest fetching follows the SMART Health Links protocol: the manifest is requested
 * with a POST carrying the recipient and (when the P flag is set) the passcode.
 */
class VhlVerifier {

    data class VhlManifestRequest(
        val url: String,
        val pin: String? = null,
        val key: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VhlDecodedLink(
        val url: String,
        val flag: String? = null,
        val key: String? = null,
        val label: String? = null,
        val exp: Long? = null
    )

    private val mapper = jacksonObjectMapper()
    private val fhirParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Decodes a VHL/SHLink URI (vhlink:/ or shlink://) to extract the manifest URL
     */
    fun decodeVhlUri(uri: String): VhlDecodedLink? {
        return try {
            val payload = when {
                uri.startsWith("vhlink://") -> uri.substring(9)
                uri.startsWith("vhlink:/") -> uri.substring(8)
                uri.startsWith("shlink://") -> uri.substring(9)
                uri.startsWith("shlink:/") -> uri.substring(8)
                else -> return null
            }

            // pad base64url payloads to a multiple of 4
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decodedJson = String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
            mapper.readValue(decodedJson, VhlDecodedLink::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a PIN is required for accessing the manifest
     */
    fun isPinRequired(decodedLink: VhlDecodedLink): Boolean {
        // P flag means passcode/PIN required
        return decodedLink.flag?.contains("P") == true
    }

    /**
     * Requests the manifest. Tries, in order: JSON POST with passcode (SHL spec),
     * form POST and GET with query parameter as fallbacks for older servers.
     */
    fun fetchManifestJson(url: String, pin: String?): JsonNode? {
        val attempts: List<() -> HttpRequest> = if (pin.isNullOrBlank()) {
            listOf(
                { jsonRequest(url, """{"recipient":"GDHCN Validator"}""") },
                { getRequest(url) },
            )
        } else {
            listOf(
                { jsonRequest(url, mapper.writeValueAsString(mapOf("recipient" to "GDHCN Validator", "passcode" to pin))) },
                { formRequest(url, "passcode=" + URLEncoder.encode(pin, "UTF-8")) },
                { getRequest(url + (if (url.contains("?")) "&" else "?") + "passcode=" + URLEncoder.encode(pin, "UTF-8")) },
            )
        }

        for (buildRequest in attempts) {
            try {
                val response = httpClient.send(buildRequest(), HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..299) {
                    return mapper.readTree(response.body())
                }
                println("VHL: manifest request to $url returned ${response.statusCode()}")
            } catch (e: Exception) {
                println("VHL: manifest request failed: ${e.message}")
            }
        }
        return null
    }

    private fun jsonRequest(url: String, body: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, application/fhir+json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(30))
        .build()

    private fun formRequest(url: String, body: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json, application/fhir+json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(30))
        .build()

    private fun getRequest(url: String) = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Accept", "application/fhir+json, application/json")
        .GET()
        .timeout(Duration.ofSeconds(30))
        .build()

    /**
     * Full content fetch: request the manifest, follow file locations or embedded
     * content, decrypt JWE payloads when the link carries a key, and parse the FHIR.
     */
    fun fetchManifest(request: VhlManifestRequest): Bundle? {
        return try {
            val manifest = fetchManifestJson(request.url, request.pin) ?: return null

            // the manifest itself may already be a FHIR resource (IHE VHL manifests)
            if (manifest.has("resourceType")) {
                return toBundle(parseFhir(manifest.toString()))
            }

            val resources = mutableListOf<Resource>()

            // embedded file contents (SHL manifest "files": [{ "embedded": ... }])
            manifest.path("files").forEach { file ->
                file.path("embedded").takeIf { it.isTextual }?.let {
                    parsePossiblyEncrypted(it.asText(), request.key)?.let(resources::add)
                }
            }

            // remote file contents
            collectUrls(manifest).forEach { fileUrl ->
                try {
                    val response = httpClient.send(getRequest(fileUrl), HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() in 200..299) {
                        parsePossiblyEncrypted(response.body(), request.key)?.let(resources::add)
                    } else {
                        println("VHL: file request to $fileUrl returned ${response.statusCode()}")
                    }
                } catch (e: Exception) {
                    println("VHL: file request to $fileUrl failed: ${e.message}")
                }
            }

            when {
                resources.isEmpty() -> null
                resources.size == 1 -> toBundle(resources.first())
                else -> Bundle().apply {
                    type = Bundle.BundleType.COLLECTION
                    resources.forEach { addEntry().resource = it }
                }
            }
        } catch (e: Exception) {
            println("VHL: manifest processing failed: ${e.message}")
            null
        }
    }

    private fun collectUrls(manifest: JsonNode): List<String> {
        val urls = mutableListOf<String>()
        manifest.path("files").forEach { file ->
            (file.path("location").takeIf { it.isTextual } ?: file.path("url").takeIf { it.isTextual })
                ?.let { urls.add(it.asText()) }
        }
        manifest.path("entries").forEach { entry ->
            entry.path("url").takeIf { it.isTextual }?.let { urls.add(it.asText()) }
        }
        manifest.path("links").forEach { link ->
            link.path("href").takeIf { it.isTextual }?.let { urls.add(it.asText()) }
        }
        return urls
    }

    /**
     * Parses a file body that is either plain FHIR JSON or a compact JWE encrypted
     * with the link's key (SHL spec: dir + A256GCM).
     */
    private fun parsePossiblyEncrypted(body: String, key: String?): Resource? {
        val trimmed = body.trim()

        val json = if (!trimmed.startsWith("{") && trimmed.count { it == '.' } == 4 && key != null) {
            try {
                val jwe = JWEObject.parse(trimmed)
                jwe.decrypt(DirectDecrypter(Base64.getUrlDecoder().decode(key)))
                jwe.payload.toString()
            } catch (e: Exception) {
                println("VHL: JWE decryption failed: ${e.message}")
                return null
            }
        } else {
            trimmed
        }

        return try {
            parseFhir(json)
        } catch (e: Exception) {
            println("VHL: could not parse FHIR content: ${e.message}")
            null
        }
    }

    private fun parseFhir(json: String): Resource {
        return fhirParser.parseResource(json) as Resource
    }

    private fun toBundle(resource: Resource): Bundle {
        if (resource is Bundle) return resource
        return Bundle().apply {
            type = Bundle.BundleType.COLLECTION
            addEntry().resource = resource
        }
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
                    size = docRef.content?.firstOrNull()?.attachment?.size?.toLong()
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
                    title = resource.resourceType.name,
                    content = resource
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
    @com.fasterxml.jackson.annotation.JsonIgnore
    val content: Any? = null // For direct FHIR content (not serialized to API responses)
)
