package org.who.gdhcnvalidator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.testing.jsonParser
import com.google.android.fhir.workflow.FhirOperator
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.who.gdhcnvalidator.services.cql.CqlBuilder
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest
import java.io.File
import java.util.*

class CQLEvaluatorTest: BaseTrustRegistryTest() {
    @get:Rule
    val fhirEngineProviderRule = FhirEngineProviderTestRule()

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val fhirEngine: FhirEngine by lazy {
        FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())
    }
    private val knowledgeManager: KnowledgeManager by lazy {
        KnowledgeManager.create(
            context = ApplicationProvider.getApplicationContext(),
            inMemory = true,
            downloadedNpmDir = (ApplicationProvider.getApplicationContext() as Context).filesDir
        ).also {
            runBlocking {
                it.index(writeToFile(ddccPass))
            }
        }
    }
    private val fhirOperator: FhirOperator by lazy {
        FhirOperator.Builder(ApplicationProvider.getApplicationContext())
            .fhirEngine(fhirEngine)
            .fhirContext(fhirContext)
            .knowledgeManager(knowledgeManager)
            .build()
    }

    private val ddccPass = CqlBuilder.compileAndBuild(inputStream("TestPass-1.0.0.cql")!!)

    private fun writeToFile(resource: BaseResource): File {
        val context: Context = ApplicationProvider.getApplicationContext()
        return File(context.cacheDir, resource.id).apply {
            val json = jsonParser.encodeResourceToString(resource)
            writeText(json)
        }
    }

    @Before
    fun setUp() = runBlocking {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    private suspend fun loadBundle(bundle: Bundle?) {
        checkNotNull(bundle)
        for (entry in bundle.entry) {
            when (entry.resource.resourceType) {
                ResourceType.Library -> knowledgeManager.index(writeToFile(entry.resource as Library))
                ResourceType.Bundle -> Unit
                else -> fhirEngine.create(entry.resource)
            }
        }
    }

    private fun patId(bundle: Bundle?): String {
        checkNotNull(bundle)
        return bundle.entry.filter { it.resource is Patient }.first().resource.id.removePrefix("Patient/")
    }

    @Ignore("Weird issue with System.arraycopy not being available")
    @Test
    fun evaluateTestPassAsCQLOnQR1FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetAllModerna"))
        assertEquals(false, results.getParameterBool("ModernaProtocol"))
        assertEquals(false, results.getParameterBool("CompletedImmunization"))
    }

    @Ignore("Weird issue with System.arraycopy not being available")
    @Test
    fun evaluateTestPassAsJSONOnQR1FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertEquals(false, results.getParameterBool("CompletedImmunization"))
    }

    @Ignore("Weird issue with System.arraycopy not being available")
    @Test
    fun evaluateTestPassAsCQLOnQR2FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST,  results.getParameters("GetFinalDose"))
        assertEquals(true, results.getParameterBool("CompletedImmunization"))
    }

    @Ignore("Weird issue with System.arraycopy not being available")
    @Test
    fun evaluateTestPassAsJSONOnQR2FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertEquals(true, results.getParameterBool("CompletedImmunization"))
    }

    @Ignore("Weird issue with System.arraycopy not being available")
    @Test
    fun evaluateTestPassAsCQLOnSHCQR1FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("SHCQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetAllModerna"))
        assertEquals(true, results.getParameterBool("ModernaProtocol"))
        assertEquals(true, results.getParameterBool("CompletedImmunization"))
    }
}