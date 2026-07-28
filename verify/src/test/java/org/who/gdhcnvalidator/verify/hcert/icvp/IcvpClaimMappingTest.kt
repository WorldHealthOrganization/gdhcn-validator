package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.hcert.CWTPayload
import org.who.gdhcnvalidator.verify.hcert.HCertVerifier

/**
 * Maps the CWT payload of a real PH4H ICVP HCERT (claim -6) to a FHIR IPS Bundle.
 */
class IcvpClaimMappingTest {
    private val emptyRegistry = object : TrustRegistry {
        override fun scopeNames(): List<TrustRegistry.ILoadedRegistry> = emptyList()
        override fun init() {}
        override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {}
        override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? = null
    }

    private val icvpCwt = """{"1":"XCL","6":1782481924,"-260":{"-6":{"n":"Jorge González","s":"male","v":{"bo":"123123123","dt":"2025-09-26","vp":"YellowFeverProductd2c75a15ed309658b3968519ddb31690","vls":"2025-09-26"},"dob":"1959-08-11","ndt":"NI","nid":"123456789"}}}"""

    @Test
    fun mapsIcvpClaimToIps() {
        val payload = HCertVerifier(emptyRegistry).mapper().readValue(icvpCwt, CWTPayload::class.java)
        val dvc = payload.data?.dvc
        assertNotNull("dvc (claim -6) should parse", dvc)
        assertEquals("Jorge González", dvc?.n?.value)
        assertEquals("YellowFeverProductd2c75a15ed309658b3968519ddb31690", dvc?.v?.vp?.value)

        val bundle = DvcMapper().run(dvc!!)
        assertNotNull("Should produce a bundle", bundle)

        val patient = bundle.entry?.map { it.resource }?.filterIsInstance<Patient>()?.firstOrNull()
        assertNotNull("Bundle should contain a Patient", patient)
        assertEquals("Jorge González", patient?.name?.firstOrNull()?.text)
        assertEquals("1959-08-11", patient?.birthDateElement?.valueAsString)

        val immunization = bundle.entry?.map { it.resource }?.filterIsInstance<Immunization>()?.firstOrNull()
        assertNotNull("Bundle should contain an Immunization", immunization)
        assertEquals("123123123", immunization?.lotNumber)
        val occurrence = (immunization?.occurrence as? org.hl7.fhir.r4.model.PrimitiveType<*>)?.valueAsString
        assertTrue("Occurrence should carry the vaccination date", occurrence?.startsWith("2025-09-26") == true)
    }
}
