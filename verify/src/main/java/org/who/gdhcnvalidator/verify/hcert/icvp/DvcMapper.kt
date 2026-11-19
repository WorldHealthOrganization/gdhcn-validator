package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.ResourceFactory
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.hl7.fhir.utilities.npm.NpmPackage
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.DualHapiWorkerContext
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.HCertDVC
import java.util.UUID

/**
 * Translates an ICVP HCERT claim (-6) into a FHIR IPS Bundle using the
 * StructureMaps published in the smart.who.int/icvp IG package.
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

        /**
         * HAPI's StructureMapUtilities cannot assign a string constant to
         * Narrative.div (ClassCastException StringType -> XhtmlType), so the
         * IG's generateNarrativeText 'setdiv' rules are removed before running.
         */
        fun stripNarrativeDivRules(map: StructureMap) {
            map.group.forEach { group ->
                group.rule.removeAll { rule ->
                    rule.target.any { it.context == "text" && it.element == "div" }
                }
            }
        }
    }

    fun run(dvm: HCertDVC): Bundle {
        val claimToLm = ivcpIG.getTransform("http://smart.who.int/icvp/StructureMap/ICVPClaimtoICVPLM")
        val lmToIps = ivcpIG.getTransform("http://smart.who.int/icvp/StructureMap/ICVPLMToIPS")
        stripNarrativeDivRules(claimToLm)
        stripNarrativeDivRules(lmToIps)

        val bundle = MyBundle().apply {
            val model = DvcLogicalModel().apply {
                myUtils.transform(ivcpIG, dvm, claimToLm, this)
            }
            myUtils.transform(ivcpIG, model, lmToIps, this)
        }

        val str = processor.encodeResourceToString(bundle)
        return processor.parseResource(str) as Bundle
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
        return when (name) {
            "http://smart.who.int/icvp/StructureDefinition/ICVP" -> DvcLogicalModel()
            "http://smart.who.int/icvp/StructureDefinition/ICVPVaccineDetails" -> DvcVaccineDetails()
            else -> ResourceFactory.createResourceOrType(name)
        }
    }

    override fun createResource(appInfo: Any?, res: Base?, atRootofTransform: Boolean): Base {
        res?.idBase = UUID.randomUUID().toString()
        return res!!;
    }

    override fun translate(appInfo: Any?, source: Coding?, conceptMapUrl: String?): Coding {
        throw FHIRException("Not implemented yet")
    }

    override fun resolveReference(appContext: Any?, url: String?): Base {
        throw FHIRException("Not implemented yet")
    }

    override fun performSearch(appContext: Any?, url: String?): MutableList<Base> {
        throw FHIRException("Not implemented yet")
    }
}
