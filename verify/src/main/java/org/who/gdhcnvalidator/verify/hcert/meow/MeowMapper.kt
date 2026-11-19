package org.who.gdhcnvalidator.verify.hcert.meow

import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.ResourceFactory
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.hl7.fhir.utilities.npm.NpmPackage
import org.who.gdhcnvalidator.verify.BaseMapper
import org.who.gdhcnvalidator.verify.DualHapiWorkerContext
import org.who.gdhcnvalidator.verify.hcert.icvp.MyBundle
import java.util.UUID

/**
 * Translates a MeOW HCERT claim (-7) into an IHE MedicationOverview Bundle
 * using the StructureMaps published in the smart.who.int/ph4h IG package.
 */
class MeowMapper: BaseMapper() {
    companion object {
        val ph4hIG = DualHapiWorkerContext.fromPackage(
            NpmPackage.fromPackage(MeowMapper::class.java.getResourceAsStream("SmartPH4H.tgz"))
        )
        val meowUtils = StructureMapUtilities(
            ph4hIG,
            MeowServices()
        )

        /**
         * HAPI's R4 engine only supports the legacy two-argument form of
         * evaluate(); the IG uses evaluate(now()), so those rules are removed
         * and the timestamps are set in code after the transform.
         */
        fun stripSingleArgEvaluateRules(map: org.hl7.fhir.r4.model.StructureMap) {
            fun strip(rules: MutableList<org.hl7.fhir.r4.model.StructureMap.StructureMapGroupRuleComponent>) {
                rules.forEach { strip(it.rule) }
                rules.removeAll { rule ->
                    rule.rule.isEmpty() && rule.dependent.isEmpty() &&
                        rule.target.all {
                            it.transform == org.hl7.fhir.r4.model.StructureMap.StructureMapTransform.EVALUATE &&
                                it.parameter.size < 2
                        } && rule.target.isNotEmpty()
                }
            }
            map.group.forEach { strip(it.rule) }
        }
    }

    fun run(claim: HCertMeow): Bundle {
        val minToLm = ph4hIG.getTransform("http://smart.who.int/ph4h/StructureMap/MedicationOverviewMinToMedicationOverviewLM")
        val lmToBundle = ph4hIG.getTransform("http://smart.who.int/ph4h/StructureMap/MedicationOverviewLMToMedicationOverviewBundle")
        stripSingleArgEvaluateRules(minToLm)
        stripSingleArgEvaluateRules(lmToBundle)

        val bundle = MyBundle().apply {
            val model = MeowLogicalModel().apply {
                meowUtils.transform(ph4hIG, claim, minToLm, this)
            }
            meowUtils.transform(ph4hIG, model, lmToBundle, this)
        }

        // set the timestamps the stripped evaluate(now()) rules would have set
        val now = java.util.Date()
        bundle.timestamp = now
        bundle.entry?.map { it.resource }?.filterIsInstance<Composition>()?.forEach { it.date = now }

        val str = processor.encodeResourceToString(bundle)
        return processor.parseResource(str) as Bundle
    }
}

class MeowServices: StructureMapUtilities.ITransformerServices {
    override fun log(message: String?) {
        println(message)
    }

    override fun createType(appInfo: Any?, name: String?): Base {
        return when (name) {
            // MEOW profiles resolve to their base FHIR resources
            "https://profiles.ihe.net/PHARM/MEOW/StructureDefinition/MedicationOverviewComposition" -> Composition()
            "https://profiles.ihe.net/PHARM/MEOW/StructureDefinition/MedicationTreatmentLine" -> MedicationStatement()
            "https://profiles.ihe.net/PHARM/MEOW/StructureDefinition/MedicationOverview" -> Bundle()
            "https://profiles.ihe.net/PHARM/MEOW/StructureDefinition/MedicationOverviewLM" -> MeowLogicalModel()
            "https://profiles.ihe.net/PHARM/MEOW/StructureDefinition/PatientLM" -> MeowPatientLM()
            "https://profiles.ihe.net/PHARM/MEOW/StructureDefinition/MedicationTreatmentLineLM" -> MeowTreatmentLineLM()
            "http://hl7.org/fhir/StructureDefinition/Patient" -> Patient()
            "http://hl7.org/fhir/StructureDefinition/Practitioner" -> Practitioner()
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
