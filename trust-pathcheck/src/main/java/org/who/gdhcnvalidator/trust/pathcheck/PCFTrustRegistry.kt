package org.who.gdhcnvalidator.trust.pathcheck

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.trust.TrustRegistry.ILoadedRegistry
import org.who.gdhcnvalidator.trust.TrustRegistry.RegistryEntity
import org.who.gdhcnvalidator.trust.TrustRegistry.TrustedEntity
import java.net.URI
import java.security.Security
import java.text.DateFormat
import java.text.SimpleDateFormat
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
class PCFTrustRegistry : TrustRegistry {
    companion object {
        const val REPO = "https://raw.githubusercontent.com/Path-Check/trust-registry/main"
        val PRODUCTION_REGISTRY =
            RegistryEntity(
                name = "PathCheck Production",
                scope = TrustRegistry.Scope.PRODUCTION,
                resolvableURI = URI("$REPO/registry_normalized.csv"),
                keyIdPrefix = "",
                publicKey = null,
            )
        val ACCEPTANCE_REGISTRY =
            RegistryEntity(
                name = "PathCheck Acceptance",
                scope = TrustRegistry.Scope.ACCEPTANCE_TEST,
                resolvableURI = URI("$REPO/test_registry_normalized.csv"),
                keyIdPrefix = "",
                publicKey = null,
            )
    }

    class LoadedRegistry(
        entity: RegistryEntity,
    ): ILoadedRegistry(entity) {
        val entries = EnumMap(
            TrustRegistry.Framework.entries.associateWith {
                mutableMapOf<String, TrustedEntity>()
            },
        )

        override fun resolve(framework: TrustRegistry.Framework, keyId: String): TrustedEntity? {
            println("${entity.name}: Resolving (active: $active) $framework $keyId")

            if (!active) return null
            return entries[framework]?.get(keyId)
        }
    }

    // Using old java.time to keep compatibility down to Android SDK 22.
    private val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    // Builds a map of all Frameworks
    private val registries = mutableListOf<LoadedRegistry>()

    private fun decode(b64: String): String = Base64.getDecoder().decode(b64).toString(Charsets.UTF_8)

    private fun parseDate(date: String): Date? = if (date.isNotEmpty()) df.parse(date) else null

    private fun wrapPem(pemB64: String): String = "-----BEGIN PUBLIC KEY-----\n$pemB64\n-----END PUBLIC KEY-----"

    fun load(registryURL: RegistryEntity) {
        try {
            val loading = LoadedRegistry(registryURL)

            // Parsing the CSV
            val (reader, elapsedServerDownload) =
                measureTimedValue {
                    registryURL.resolvableURI
                        .toURL()
                        .openStream()
                        .bufferedReader()
                }
            println("TIME: Trust Downloaded in $elapsedServerDownload from ${registryURL.resolvableURI}")

            val elapsed =
                measureTimeMillis {
                    reader.forEachLine {
                        val (
                            specName, kid, status, displayNameB64, displayLogoB64,
                            validFromISOStr, validUntilISOStr, publicKey,
                        ) = it.split(",")

                        try {
                            loading.entries[TrustRegistry.Framework.valueOf(specName.uppercase())]
                                ?.put(
                                    kid,
                                    TrustRegistry.TrustedEntity(
                                        mapOf("en" to decode(displayNameB64)),
                                        decode(displayLogoB64),
                                        TrustRegistry.Status.valueOf(status.uppercase()),
                                        registryURL.scope,
                                        parseDate(validFromISOStr),
                                        parseDate(validUntilISOStr),
                                        KeyUtils.publicKeyFromPEM(wrapPem(publicKey)),
                                    ),
                                )
                        } catch (t: Throwable) {
                            println("Exception while loading kid: $specName $kid")
                            t.printStackTrace()
                        }
                    }
                }

            registries.add(loading)

            println("TIME: Trust Parsed and Loaded in ${elapsed}ms")
        } catch (t: Throwable) {
            println("Exception while loading registry from github")
            t.printStackTrace()
        }
    }

    override fun init(vararg customRegistries: RegistryEntity) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        registries.clear()

        customRegistries.forEach {
            load(it)
        }
    }

    override fun scopeNames(): List<LoadedRegistry> {
        return registries
    }

    override fun init() {
        println("PathCheck: Initializing")
        init(PRODUCTION_REGISTRY, ACCEPTANCE_REGISTRY)
    }

    override fun resolve(
        framework: TrustRegistry.Framework,
        kid: String,
    ): TrustedEntity? {
        println("PathCheck: Resolving $framework $kid")
        return registries.firstNotNullOfOrNull {
            it.resolve(framework, kid)
        }
    }
}
