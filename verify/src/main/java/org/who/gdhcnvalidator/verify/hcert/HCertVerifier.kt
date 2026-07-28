package org.who.gdhcnvalidator.verify.hcert

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import nl.minvws.encoding.Base45
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.who.gdhcnvalidator.QRDecoder
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.hcert.dcc.DccMapper
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.DdccCoreDataSetTR
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.DdccCoreDataSetVS
import org.who.gdhcnvalidator.verify.hcert.ddcc.DdccMapper
import org.who.gdhcnvalidator.verify.hcert.ddcc.ReferenceDeserializer
import org.who.gdhcnvalidator.verify.hcert.healthlink.SmartHealthLinkModel
import org.who.gdhcnvalidator.verify.hcert.healthlink.VhlVerifier
import org.who.gdhcnvalidator.verify.hcert.icvp.DvcMapper
import org.who.gdhcnvalidator.verify.hcert.meow.MeowMapper
import java.net.URLDecoder
import java.security.PublicKey
import java.util.*
import java.util.zip.InflaterInputStream
import kotlin.time.measureTimedValue


/**
 * Turns HC1 QR Codes into Fhir Objects
 */
class HCertVerifier (private val registry: TrustRegistry) {
    private val prefix = "HC1:"

    private fun prefixDecode(qr: String): String {
        return when {
            qr.startsWith(prefix) -> qr.drop(prefix.length)
            else -> qr
        }
    }

    private fun base45Decode(base45: String): ByteArray? {
        return try {
            Base45.getDecoder().decode(base45)
        } catch (e: Throwable) {
            null
        }
    }

    private fun deflate(input: ByteArray): ByteArray? {
        return try {
            InflaterInputStream(input.inputStream()).readBytes()
        } catch (e: Throwable) {
            null
        }
    }

    private fun decodeSignedMessage(input: ByteArray): Sign1Message? {
        return try {
            Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
        } catch (e: Throwable) {
            null
        }
    }

    private fun getKID(input: Sign1Message): String? {
        val kid = input.protectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]
               ?: input.unprotectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]
               ?: return null
        return when (kid.type) {
            CBORType.ByteString -> Base64.getEncoder().encodeToString(kid.GetByteString())
            // some wallets encode the kid as a text string (already base64)
            CBORType.TextString -> kid.AsString()
            else -> null
        }
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return registry.resolve(TrustRegistry.Framework.DCC, kid)
    }

    private fun getContent(signedMessage: Sign1Message): CBORObject {
        return CBORObject.DecodeFromBytes(signedMessage.GetContent())
    }

    private fun verify(signedMessage: Sign1Message, pubKey: PublicKey): Boolean {
        return try {
            val (verified, elapsedStructureMapLoad) = measureTimedValue {
                val key = OneKey(pubKey, null)
                signedMessage.validate(key)
            }
            println("TIME: Verify $elapsedStructureMapLoad")

            return verified
        } catch (e: Throwable) {
            false
        }
    }

    fun unpack(qr: String): String? {
        val hc1Decoded = prefixDecode(qr)
        val decodedBytes = base45Decode(hc1Decoded) ?: return null
        val deflatedBytes = deflate(decodedBytes) ?: return null
        val signedMessage = decodeSignedMessage(deflatedBytes) ?: return null
        return unpack(signedMessage, getContent(signedMessage))
    }

    val EU_DCC_CODE = -260
    val COUNTRY_CODE = 1

    private fun getCountry(hcertPayload: CBORObject): String? {
        // some wallets encode the CWT claim keys as text ("1") instead of int (1)
        val country = hcertPayload[COUNTRY_CODE] ?: hcertPayload["1"]
        return country?.AsString()?.lowercase()
    }

    fun mapper(): ObjectMapper {
        val module = SimpleModule()
        module.addDeserializer(Reference::class.java, ReferenceDeserializer)
        // explicit deserializer: Jackson's implicit single-String-constructor discovery
        // for HAPI primitives varies between versions (silent null on 2.17)
        module.addDeserializer(StringType::class.java, object : JsonDeserializer<StringType>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = StringType(p.valueAsString)
        })
        return jacksonObjectMapper().registerModule(module)
    }

    fun toFhir(hcertPayload: CBORObject): Bundle? {
        val hcert = hcertPayload[EU_DCC_CODE]
        if (hcert != null) {
            try {
                val payload = mapper().readValue(
                    hcertPayload.ToJSONString(),
                    CWTPayload::class.java
                )

                if (payload.data?.dcc != null) {
                    return DccMapper().run(payload)
                }

                payload.data?.coreDataSetVS?.let {
                    return DdccMapper().run(it)
                }

                payload.data?.coreDataSetTR?.let {
                    return DdccMapper().run(it)
                }

                payload.data?.dvc?.let {
                    return DvcMapper().run(it)
                }

                payload.data?.meow?.let {
                    return MeowMapper().run(it)
                }
            } catch (e: Exception) {
                println("error on: "+ hcertPayload.ToJSONString())
                e.printStackTrace()
            }
        }

        // hacks from previous versions
        try {
            return DdccMapper().run(
                jacksonObjectMapper().readValue(
                    hcertPayload.ToJSONString(),
                    DdccCoreDataSetVS::class.java
                )
            );
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            return DdccMapper().run(
                jacksonObjectMapper().readValue(
                    hcertPayload.ToJSONString(),
                    DdccCoreDataSetTR::class.java
                )
            );
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun unpackAndVerify(qr: String, pin: String? = null): QRDecoder.VerificationResult {
        val hc1Decoded = prefixDecode(qr)
        val decodedBytes = base45Decode(hc1Decoded) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, qr, null)
        val deflatedBytes = deflate(decodedBytes) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_COMPRESSION, null, null, qr, null)
        val signedMessage = decodeSignedMessage(deflatedBytes) ?: return QRDecoder.VerificationResult(
            QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, qr, null)

        val contentsCBOR = getContent(signedMessage)
        val unpacked = unpack(signedMessage, contentsCBOR)

        // VHL/SHLink references (claim 5) bypass the FHIR mapping: the signed
        // content is the link itself; the health data is fetched from the manifest.
        val healthLink = getHealthLink(contentsCBOR)

        val contents = if (healthLink != null) null
            else toFhir(contentsCBOR) ?: return QRDecoder.VerificationResult(QRDecoder.Status.NOT_SUPPORTED, null, null, qr, unpacked)

        val kid = getKID(signedMessage) ?: return QRDecoder.VerificationResult(QRDecoder.Status.KID_NOT_INCLUDED, contents, null, qr, unpacked)
        val decodedKid = URLDecoder.decode(kid, "UTF-8")
        val countryCode = getCountry(contentsCBOR)

        // try new key ids first
        val issuer = countryCode?.let { resolveIssuer("$countryCode#$kid") }
            ?: resolveIssuer(kid)
            ?: return QRDecoder.VerificationResult(QRDecoder.Status.ISSUER_NOT_TRUSTED, contents, null, qr, unpacked)

        return when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> QRDecoder.VerificationResult(QRDecoder.Status.TERMINATED_KEYS, contents, issuer, qr, unpacked)
            TrustRegistry.Status.EXPIRED -> QRDecoder.VerificationResult(QRDecoder.Status.EXPIRED_KEYS, contents, issuer, qr, unpacked)
            TrustRegistry.Status.REVOKED -> QRDecoder.VerificationResult(QRDecoder.Status.REVOKED_KEYS, contents, issuer, qr, unpacked)
            TrustRegistry.Status.CURRENT ->
                if (verify(signedMessage, issuer.publicKey))
                    if (healthLink != null)
                        resolveHealthLink(healthLink, issuer, qr, unpacked, pin)
                    else
                        QRDecoder.VerificationResult(QRDecoder.Status.VERIFIED, contents, issuer, qr, unpacked)
                else
                    QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNATURE, contents, issuer, qr, unpacked)
        }
    }

    private fun getHealthLink(hcertPayload: CBORObject): SmartHealthLinkModel? {
        return try {
            mapper().readValue(hcertPayload.ToJSONString(), CWTPayload::class.java)
                .data?.healthLink?.firstOrNull { it.getUri() != null }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The HCERT signature over the link is already verified; decode the link and,
     * when allowed (no PIN required, or PIN provided), fetch the manifest content.
     */
    private fun resolveHealthLink(
        link: SmartHealthLinkModel,
        issuer: TrustRegistry.TrustedEntity,
        qr: String,
        unpacked: String?,
        pin: String?,
    ): QRDecoder.VerificationResult {
        val vhl = VhlVerifier()
        val decoded = vhl.decodeVhlUri(link.getUri()!!)
            ?: return QRDecoder.VerificationResult(QRDecoder.Status.VHL_INVALID_URI, null, issuer, qr, unpacked)

        val requiresPin = vhl.isPinRequired(decoded)
        val vhlInfo = QRDecoder.VhlInfo(decodedLink = decoded, requiresPin = requiresPin)

        if (requiresPin && pin.isNullOrBlank()) {
            return QRDecoder.VerificationResult(QRDecoder.Status.VHL_REQUIRES_PIN, null, issuer, qr, unpacked, vhlInfo)
        }

        val manifest = vhl.fetchManifest(VhlVerifier.VhlManifestRequest(decoded.url, pin, decoded.key))
            ?: return QRDecoder.VerificationResult(QRDecoder.Status.VHL_FETCH_ERROR, null, issuer, qr, unpacked, vhlInfo)

        val fileList = vhl.extractFileList(manifest)
        return QRDecoder.VerificationResult(
            QRDecoder.Status.VERIFIED, manifest, issuer, qr, unpacked,
            vhlInfo.copy(fileList = fileList)
        )
    }

    private fun unpack(signedMessage: Sign1Message, contents: CBORObject): String? {
        return """
            {
                "protectedAttributes": ${signedMessage.protectedAttributes.ToJSONString()},
                "unprotectedAttributes": ${signedMessage.unprotectedAttributes.ToJSONString()},
                "contents": ${contents.ToJSONString()}
            }
        """;
    }
}