package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.hcert.CWTPayload
import org.who.gdhcnvalidator.verify.hcert.HCertVerifier

/**
 * Parses the CWT payload of a real PH4H VHL HCERT (claim 5 with a shlink:// reference)
 */
class HCertVhlClaimParseTest {
    private val emptyRegistry = object : TrustRegistry {
        override fun scopeNames(): List<TrustRegistry.ILoadedRegistry> = emptyList()
        override fun init() {}
        override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {}
        override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? = null
    }

    private val track1Cwt = """{"1":"XL","4":1783723963,"6":1782481969732,"-260":{"5":[{"u":"shlink://eyJ1cmwiOiJodHRwOi8vbGFjcGFzcy5jcmVhdGUuY2w6ODE4Mi92Mi9tYW5pZmVzdHMvMDFmMGE4ZTUtMWU5NC00ZWM0LTliMzEtYWM5NmIyMDhmMTI2IiwiZmxhZyI6IlAiLCJleHAiOjE3ODM3MjM5NjM5OTcsImtleSI6ImVQaG1KMS1mSnY0d19jbWZSTWU5aW5vVGZENER3MGRNazNCR0x3Vi1LZTA9IiwibGFiZWwiOiJHREhDTiBWYWxpZGF0b3IifQ=="}]}}"""

    @Test
    fun parsesHealthLinkClaim() {
        val payload = HCertVerifier(emptyRegistry).mapper().readValue(track1Cwt, CWTPayload::class.java)

        assertNotNull("data (-260) should parse", payload.data)
        assertNotNull("healthLink (claim 5) should parse", payload.data?.healthLink)
        assertEquals(1, payload.data?.healthLink?.size)

        val uri = payload.data?.healthLink?.first()?.getUri()
        assertNotNull("link uri should be present", uri)
        assertEquals(true, uri!!.startsWith("shlink://"))
    }
}
