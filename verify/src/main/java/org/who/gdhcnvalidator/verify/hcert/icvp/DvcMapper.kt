package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.ResourceFactory
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.hl7.fhir.utilities.npm.NpmPackage
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.DualHapiWorkerContext
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.HCertDVC
import java.util.UUID
import kotlin.time.measureTimedValue

/**
 * Translates a QR CBOR object into FHIR Objects
 */
class DvcMapper: BaseMapper() {
    companion object {
        val ivcpIG = DualHapiWorkerContext.fromPackage(
            NpmPackage.fromPackage(DvcMapper::class.java.getResourceAsStream("SmartICVP.tgz"))
        ).apply {
            this.cacheResource(DvcLogicalModel())
        }
        val myUtils = StructureMapUtilities(
            ivcpIG,
            MyServices()
        )
    }

    fun run(dvm: HCertDVC): Bundle {
        println("starting structures " + ivcpIG.allStructures().joinToString(", ") { it.name })
        println("starting maps " + ivcpIG.listTransforms().joinToString(", ") { it.name })
        println("starting resources " + ivcpIG.resourceNames.joinToString(", ") { it })
        println("starting types " + ivcpIG.typeNames.joinToString(", ") { it })

        println("starting maps " + ivcpIG.isResource("http://smart.who.int/icvp/StructureDefinition/ModelDVC"))

        val map = ivcpIG.getTransform("http://smart.who.int/icvp/StructureMap/DVCClaimtoIPS")

        val map1 = ivcpIG.getTransform("http://smart.who.int/icvp/StructureMap/DVCClaimtoDVCLM")
        val map2 = ivcpIG.getTransform("http://smart.who.int/icvp/StructureMap/DVCLMToIPS")

        val bundle = MyBundle().apply {
            // TODO: for some reason it doesn't support this full map
            //myUtils.transform(ivcpIG, dvm, map, this)
            val model = DvcLogicalModel().apply {
                myUtils.transform(ivcpIG, dvm, map1, this)
            }
            myUtils.transform(ivcpIG, model, map2, this)
        }

        val (bundle2, elapsedSerialization) = measureTimedValue {
            val str = processor.encodeResourceToString(bundle)
            processor.parseResource(str) as Bundle
        }

        return bundle2
    }
}

class MyBundle: Bundle() {
    override fun castToInstant(b: Base?): InstantType? {
        if (b == null) {
            return null
        }
        if (b is InstantType) return b
        if (b is DateTimeType) return InstantType(b)

        else throw FHIRException("Unable to convert a " + b.javaClass.name + " to a Instant")
    }
}

class MyServices: StructureMapUtilities.ITransformerServices {
    override fun log(message: String?) {
        println(message)
    }

    override fun createType(appInfo: Any?, name: String?): Base {
        println("MyServices: " + "createType $name")

        return when (name) {
            "http://smart.who.int/icvp/StructureDefinition/ModelDVC" -> DvcLogicalModel()
            else -> ResourceFactory.createResourceOrType(name)
        }
    }

    override fun createResource(appInfo: Any?, res: Base?, atRootofTransform: Boolean): Base {
        println("MyServices: " + "createResource $appInfo $res $atRootofTransform")
        res?.idBase = UUID.randomUUID().toString()
        return res!!;
    }

    override fun translate(appInfo: Any?, source: Coding?, conceptMapUrl: String?): Coding {
        println("MyServices: " + "translate")
        throw FHIRException("Not implemented yet")
    }

    override fun resolveReference(appContext: Any?, url: String?): Base {
        println("MyServices: " + "resolveReference")
        throw FHIRException("Not implemented yet")
    }

    override fun performSearch(appContext: Any?, url: String?): MutableList<Base> {
        println("MyServices: " + "performSearch")
        throw FHIRException("Not implemented yet")
    }
}