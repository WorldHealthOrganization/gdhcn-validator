package org.who.gdhcnvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.r4.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.who.gdhcnvalidator.services.cql.CqlBuilder
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest
import java.util.*

@RunWith(RobolectricTestRunner::class)
class CQLCompilerTest: BaseTrustRegistryTest() {
    @get:Rule
    val fhirEngineProviderRule = FhirEngineProviderTestRule()

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val availableIGs = listOf(
        "DDCCPass-1.0.0.cql",
        "AnyDosePass-1.0.0.cql",
        "ModernaOrPfizerPass-1.0.0.cql"
    )

    private fun compileIGs(): List<Library> {
        val libs = availableIGs.map { CqlBuilder.compileAndBuild(inputStream(it)!!) }
        libs.forEach {
            println("Library: " + jSONParser.encodeResourceToString(it))
        }
        return libs
    }

    @Test
    fun compileAvailableLibraries() {
        assertEquals(3, compileIGs().size)
    }
}