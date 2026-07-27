package org.who.gdhcnvalidator.verify.hcert.meow

import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.hcert.CWTPayload
import org.who.gdhcnvalidator.verify.hcert.HCertVerifier

/**
 * Maps the CWT payload of a real PH4H MeOW HCERT (claim -7) to a
 * MedicationOverview document Bundle.
 */
class MeowClaimMappingTest {
    private val emptyRegistry = object : TrustRegistry {
        override fun scopeNames(): List<TrustRegistry.ILoadedRegistry> = emptyList()
        override fun init() {}
        override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {}
        override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? = null
    }

    private val meowCwt = """{"1":"XCL","4":1813586030,"6":1782482030,"-260":{"-7":{"m":[{"d":"1 comp. por día","m":"318956006","r":"Product containing precisely losartan potassium 50 milligram/1 each conventional release oral tablet (clinical drug)","da":"2025-06-17","ee":"2025-12-31","es":"2025-01-01"}],"n":"Jorge González","s":"male","dt":"NI","id":"123456789","dob":"1959-08-11"}}}"""

    @Test
    fun mapsMeowClaimToMedicationOverview() {
        val payload = HCertVerifier(emptyRegistry).mapper().readValue(meowCwt, CWTPayload::class.java)
        val meow = payload.data?.meow
        assertNotNull("meow (claim -7) should parse", meow)
        assertEquals("Jorge González", meow?.n?.value)
        assertEquals(1, meow?.m?.size)
        assertEquals("318956006", meow?.m?.first()?.m?.value)

        val bundle = MeowMapper().run(meow!!)
        assertNotNull("Should produce a bundle", bundle)

        val resources = bundle.entry?.map { it.resource } ?: emptyList()
        val patient = resources.filterIsInstance<Patient>().firstOrNull()
        assertNotNull("Bundle should contain a Patient", patient)
        assertEquals("Jorge González", patient?.name?.firstOrNull()?.text)
        assertEquals("1959-08-11", patient?.birthDateElement?.valueAsString)

        val statement = resources.filterIsInstance<MedicationStatement>().firstOrNull()
        assertNotNull("Bundle should contain a MedicationStatement", statement)
        assertEquals("318956006", statement?.medicationCodeableConcept?.codingFirstRep?.code)
        assertEquals("1 comp. por día", statement?.dosageFirstRep?.text)

        assertNotNull("Bundle should contain a Composition",
            resources.filterIsInstance<Composition>().firstOrNull())
    }
}
