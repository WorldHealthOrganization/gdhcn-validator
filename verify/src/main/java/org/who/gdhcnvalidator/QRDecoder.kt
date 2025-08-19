package org.who.gdhcnvalidator

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.divoc.DivocVerifier
import org.who.gdhcnvalidator.verify.hcert.HCertVerifier
import org.who.gdhcnvalidator.verify.hcert.healthlink.VhlVerifier
import org.who.gdhcnvalidator.verify.icao.IcaoVerifier
import org.who.gdhcnvalidator.verify.shc.ShcVerifier

/**
 * Finds the right processor for the QR Content and returns the Composition of that Content.
 */
class QRDecoder(private val registry: TrustRegistry) {
    enum class Status {
        NOT_FOUND, // QR not found in the Image
        NOT_SUPPORTED, // QR Standard not supported by this algorithm
        INVALID_ENCODING, // could not decode Base45 for DCC, Base10 for SHC,
        INVALID_COMPRESSION, // could not decompress the byte array
        INVALID_SIGNING_FORMAT, // invalid COSE, JOSE, W3C VC Payload
        KID_NOT_INCLUDED, // unable to resolve the issuer ID
        ISSUER_NOT_TRUSTED, // issuer is not found in the registry
        TERMINATED_KEYS, // issuer was terminated by the registry
        EXPIRED_KEYS, // keys expired
        REVOKED_KEYS, // keys were revoked by the issuer
        INVALID_SIGNATURE, // signature doesn't match
        VERIFIED,  // Verified content.
        VHL_REQUIRES_PIN, // VHL requires PIN entry
        VHL_INVALID_URI, // VHL URI could not be decoded
        VHL_FETCH_ERROR, // VHL manifest could not be fetched
    }

    data class VerificationResult (
        var status: Status,
        var contents: Bundle?, // the Composition
        var issuer: TrustRegistry.TrustedEntity?,
        var qr: String,
        var unpacked: String?,
        var vhlInfo: VhlInfo? = null // Additional VHL-specific information
    ) {
        fun composition() = contents?.entry?.filter { it.resource is Composition }?.firstOrNull()?.resource as Composition?
    }
    
    /**
     * Additional information for VHL processing
     */
    data class VhlInfo(
        val decodedLink: VhlVerifier.VhlDecodedLink?,
        val requiresPin: Boolean = false,
        val fileList: List<VhlVerifier.VhlFileInfo>? = null
    )

    fun decode(qrPayload : String): VerificationResult {
        // Check for VHL/SHL URIs first
        if (qrPayload.startsWith("vhlink:/") || qrPayload.startsWith("shlink:/")) {
            return processVhlUri(qrPayload)
        }
        
        if (qrPayload.uppercase().startsWith("HC1:")) {
            return HCertVerifier(registry).unpackAndVerify(qrPayload)
        }
        if (qrPayload.uppercase().startsWith("SHC:")) {
            return ShcVerifier(registry).unpackAndVerify(qrPayload)
        }
        if (qrPayload.uppercase().startsWith("B64:") || qrPayload.uppercase().startsWith("PK")) {
            return DivocVerifier(registry).unpackAndVerify(qrPayload)
        }
        if (qrPayload.uppercase().contains("ICAO")) {
            return IcaoVerifier(registry).unpackAndVerify(qrPayload)
        }

        return VerificationResult(Status.NOT_SUPPORTED, null, null, qrPayload, null)
    }
    
    /**
     * Process VHL URI and return initial verification result
     * Full VHL processing with PIN entry and manifest fetching happens in the UI layer
     */
    private fun processVhlUri(qrPayload: String): VerificationResult {
        val vhlVerifier = VhlVerifier()
        val decodedLink = vhlVerifier.decodeVhlUri(qrPayload)
        
        return if (decodedLink != null) {
            val requiresPin = vhlVerifier.isPinRequired(decodedLink)
            val vhlInfo = VhlInfo(
                decodedLink = decodedLink,
                requiresPin = requiresPin
            )
            
            if (requiresPin) {
                VerificationResult(
                    status = Status.VHL_REQUIRES_PIN,
                    contents = null,
                    issuer = null,
                    qr = qrPayload,
                    unpacked = null,
                    vhlInfo = vhlInfo
                )
            } else {
                // Try to fetch manifest without PIN
                val manifest = vhlVerifier.fetchManifest(VhlVerifier.VhlManifestRequest(decodedLink.url))
                if (manifest != null) {
                    val fileList = vhlVerifier.extractFileList(manifest)
                    VerificationResult(
                        status = Status.VERIFIED,
                        contents = manifest,
                        issuer = null,
                        qr = qrPayload,
                        unpacked = decodedLink.url,
                        vhlInfo = vhlInfo.copy(fileList = fileList)
                    )
                } else {
                    VerificationResult(
                        status = Status.VHL_FETCH_ERROR,
                        contents = null,
                        issuer = null,
                        qr = qrPayload,
                        unpacked = decodedLink.url,
                        vhlInfo = vhlInfo
                    )
                }
            }
        } else {
            VerificationResult(
                status = Status.VHL_INVALID_URI,
                contents = null,
                issuer = null,
                qr = qrPayload,
                unpacked = null
            )
        }
    }
}