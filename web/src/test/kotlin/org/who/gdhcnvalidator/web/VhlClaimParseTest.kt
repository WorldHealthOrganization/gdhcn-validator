package org.who.gdhcnvalidator.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.hcert.CWTPayload
import org.who.gdhcnvalidator.verify.hcert.HCertVerifier

/**
 * Regression test for the HCERT claim-5 (VHL) parse under the web module's
 * Jackson version (Spring Boot manages a different Jackson than :verify tests;
 * 2.17 silently dropped HAPI StringType creator values).
 */
class VhlClaimParseTest {
    private val emptyRegistry = object : TrustRegistry {
        override fun scopeNames(): List<TrustRegistry.ILoadedRegistry> = emptyList()
        override fun init() {}
        override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {}
        override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? = null
    }

    private val vhlCwt = """{"1":"XL","4":1783723963,"6":1782481969732,"-260":{"5":[{"u":"shlink://eyJ1cmwiOiJodHRwOi8vbGFjcGFzcy5jcmVhdGUuY2w6ODE4Mi92Mi9tYW5pZmVzdHMvMDFmMGE4ZTUtMWU5NC00ZWM0LTliMzEtYWM5NmIyMDhmMTI2IiwiZmxhZyI6IlAiLCJleHAiOjE3ODM3MjM5NjM5OTcsImtleSI6ImVQaG1KMS1mSnY0d19jbWZSTWU5aW5vVGZENER3MGRNazNCR0x3Vi1LZTAiLCJsYWJlbCI6IkdESENOIFZhbGlkYXRvciJ9"}]}}"""

    @Test
    fun parsesHealthLinkClaim() {
        val payload = HCertVerifier(emptyRegistry).mapper().readValue(vhlCwt, CWTPayload::class.java)

        assertNotNull(payload.data, "data (-260) should parse")
        assertNotNull(payload.data?.healthLink, "healthLink (claim 5) should parse")
        assertEquals(1, payload.data?.healthLink?.size)
        assertEquals(true, payload.data?.healthLink?.first()?.getUri()?.startsWith("shlink://"))
    }
}
