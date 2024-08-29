package org.who.gdhcnvalidator.trust.didweb

import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.JWK
import foundation.identity.did.VerificationMethod
import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.who.gdhcnvalidator.trust.TrustRegistry
import java.net.URI
import java.net.URLEncoder
import java.security.PublicKey
import java.security.Security
import java.text.ParseException
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]
operator fun <T> List<T>.component8() = this[7]

/**
 * Resolve Keys for Verifiers from PathCheck's CSV file
 */
class GDHCNTrustRegistry : TrustRegistry {
    companion object {
        const val PROD_KEY_ID = "did:web:tng-cdn.who.int:trustlist"
        const val TEST_KEY_ID = "did:web:tng-cdn-uat.who.int:trustlist"
        const val DEV_KEY_ID = "did:web:tng-cdn-dev.who.int:trustlist"

        const val PROD_DID = "https://tng-cdn.who.int/trustlist/did.json"
        const val ACCEPTANCE_DID = "https://tng-cdn-uat.who.int/trustlist/did.json"
        const val DEV_DID = "https://tng-cdn-dev.who.int/trustlist/did.json"

        val PRODUCTION_REGISTRY = TrustRegistry.RegistryEntity(TrustRegistry.Scope.PRODUCTION, URI(PROD_DID), PROD_KEY_ID, null)
        val ACCEPTANCE_REGISTRY =  TrustRegistry.RegistryEntity(TrustRegistry.Scope.ACCEPTANCE_TEST, URI(ACCEPTANCE_DID), TEST_KEY_ID, null)
        val DEV_STAGING_REGISTRY =  TrustRegistry.RegistryEntity(TrustRegistry.Scope.DEV_STAGING, URI(DEV_DID), DEV_KEY_ID, null)
    }

    // Builds a map of all Frameworks
    private val registry = mutableMapOf<URI, TrustRegistry.TrustedEntity>()
    private val prefixes = mutableListOf<String>()

    private fun wrapPem(pemB64: String): String {
        return "-----BEGIN PUBLIC KEY-----\n$pemB64\n-----END PUBLIC KEY-----"
    }

    private fun buildPublicKey(verif: VerificationMethod): PublicKey? {
        if (verif.publicKeyJwk != null) {
            try {
                val key = JWK.parse(verif.publicKeyJwk)
                if (key is AsymmetricJWK) {
                    return key.toPublicKey()
                }
            } catch (e: ParseException) {
                // tries to reassemble the public key from the first certificate
                if (verif.publicKeyJwk.containsKey("x5c")) {
                    val certPem = (verif.publicKeyJwk.get("x5c") as List<*>).firstOrNull()
                    return KeyUtils.certificatePublicKeyFromPEM(
                        "-----BEGIN CERTIFICATE-----\n$certPem\n-----END CERTIFICATE-----"
                    )
                } else {
                    throw e
                }
            }
        }

        if (verif.publicKeyBase64 != null) {
            val key = wrapPem(verif.publicKeyBase64)
            return KeyUtils.publicKeyFromPEM(key)
        }

        if (verif.publicKeyBase58 != null) {
            return KeyUtils.eddsaFromBytes(Base58.decode(verif.publicKeyBase58))
        }

        if (verif.publicKeyMultibase != null) {
            return KeyUtils.eddsaFromBytes(Multibase.decode(verif.publicKeyMultibase))
        }

        println("unable to load key ${verif.id}")

        return null
    }

    fun load(registryURL: TrustRegistry.RegistryEntity) {
        try {
            val (didDocumentResolution, elapsedServerDownload) = measureTimedValue {
                DIDWebResolver().resolve(registryURL.resolvableURI)
            }
            println("TIME: Trust Downloaded in $elapsedServerDownload from ${registryURL.resolvableURI}")

            val elapsed = measureTimeMillis {
                didDocumentResolution?.didDocument?.verificationMethods?.forEach {
                    try {
                        val key = buildPublicKey(it)
                        if (key != null)
                            registry.put(it.id,
                                TrustRegistry.TrustedEntity(
                                    mapOf("en" to it.id.toString()),
                                    "",
                                    TrustRegistry.Status.CURRENT,
                                    registryURL.scope,
                                    null,
                                    null,
                                    key
                                )
                            )
                    } catch(t: Throwable) {
                        println("Exception while loading kid: ${it.id}")
                        t.printStackTrace()
                    }
                }
            }

            prefixes.add(registryURL.keyIdPrefix)

            println("TIME: Trust Parsed and Loaded in ${elapsed}ms")

        } catch(t: Throwable) {
            println("Exception while loading registry from github")
            t.printStackTrace()
        }
    }

    override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        registry.clear()

        customRegistries.forEach {
            load(it)
        }
    }

    override fun init() {
        println("DID:WEB: Initializing")
        init(PRODUCTION_REGISTRY, ACCEPTANCE_REGISTRY, DEV_STAGING_REGISTRY)
    }

    override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? {
        if (kid.contains("#")) {
            val parts = kid.split("#")
            val encController = URLEncoder.encode(parts[0],"UTF-8")
            val encKid = URLEncoder.encode(parts[1],"UTF-8")
            return prefixes.firstNotNullOfOrNull {
                println("DID:WEB: Resolving $kid -> $it:$encController#$encKid")
                registry[URI.create("$it:$encController#$encKid")]
            }
        } else {
            val encKid = URLEncoder.encode(kid,"UTF-8")

            return prefixes.firstNotNullOfOrNull {
                println("DID:WEB: Resolving $kid -> $it:$encKid#$encKid")
                registry[URI.create("$it:$encKid#$encKid")]
            }
        }
    }
}
