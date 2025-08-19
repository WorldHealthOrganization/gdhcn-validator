package org.who.gdhcnvalidator.verify

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.fhir.ucum.UcumEssenceService
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.hl7.fhir.utilities.npm.NpmPackage
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.time.measureTimedValue

class DualHapiWorkerContext : SimpleWorkerContext() {
    val hapi =
        HapiWorkerContext(
            FhirContext.forR4Cached(),
            FhirContext.forR4Cached().validationSupport,
        )

    init {
        // TODO: get this service from Common: UnitConverter.ucumService
        this.ucumService = UcumEssenceService(this::class.java.getResourceAsStream("/ucum-essence.xml"))
    }

    override fun <T : Resource?> fetchResourceWithException(class_: Class<T>?, uri: String?): T {
        val res = try {
            hapi.fetchResourceWithException(class_, uri)
        } catch (e: Exception) {
            null
        }

        return res ?: super.fetchResourceWithException(class_, uri)
    }

    companion object {
        fun fromPackage(pi: NpmPackage?): DualHapiWorkerContext {
            val res = DualHapiWorkerContext()
            res.loadFromPackage(pi, null)
            return res
        }
    }
}


open class BaseMapper {
    companion object {
        val processor = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
        val simpleWorkerContext = DualHapiWorkerContext()
        val utils = StructureMapUtilities(simpleWorkerContext)
        val structureMapCache = hashMapOf<String, StructureMap>()
    }

    fun loadFile(mapFileName: String): InputStream? {
        return javaClass.getResourceAsStream(mapFileName)
    }

    fun addCache(mapFileName: String, mappingText: String) {
        structureMapCache.put(mapFileName, utils.parse(mappingText, ""))
    }

    fun loadMap(mapFileName: String): StructureMap? {
        if (!structureMapCache.containsKey(mapFileName)) {
            loadFile(mapFileName)?.let {
                addCache(mapFileName, it.bufferedReader().readText())
            }
        }

        return structureMapCache[mapFileName]
    }

    fun run(source: Base, mapFileName: String): Bundle {
        return run<Bundle>(source, mapFileName, Bundle(), simpleWorkerContext)
    }

    fun <T: Resource> run(source: Base, mapFileName: String, target: T): T {
        return run<T>(source, mapFileName, target, simpleWorkerContext)
    }

    fun run(source: Base, mapFileName: String, worker: SimpleWorkerContext): Bundle {
        return run<Bundle>(source, mapFileName, Bundle(), worker)
    }

    fun <T: Resource> run(source: Base, mapFileName: String, target: T, worker: SimpleWorkerContext): T {
        val (structureMap, elapsedStructureMapLoad) = measureTimedValue {
            loadMap(mapFileName)
        }
        println("TIME: StructureMap Loaded in $elapsedStructureMapLoad")

        val (output, elapsedTransform) = measureTimedValue {
            target.apply {
                utils.transform(worker, source, structureMap, this)
            }
        }
        println("TIME: StructureMap Applied in $elapsedTransform")

        val (output2, elapsedSerialization) = measureTimedValue {
            val str = processor.encodeResourceToString(output)
            processor.parseResource(str) as T
        }
        println("TIME: Resource re-organized in $elapsedSerialization")

        return output2
    }
}