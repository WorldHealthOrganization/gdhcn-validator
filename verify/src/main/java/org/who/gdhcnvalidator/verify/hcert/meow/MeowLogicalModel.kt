package org.who.gdhcnvalidator.verify.hcert.meow

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel

/**
 * Logical models mirroring the IHE PHARM MEOW logical models
 * (https://profiles.ihe.net/PHARM/MEOW/) used as the intermediate
 * representation between the HCERT claim and the MedicationOverview Bundle.
 *
 * All element names referenced by the PH4H StructureMaps are declared
 * (even when unused) so the transform engine can probe them safely.
 */
class MeowLogicalModel(
    var patient: MeowPatientLM? = null,
    var medicationTreatmentLine: MutableList<MeowTreatmentLineLM> = mutableListOf(),
    var verification: BaseModel? = null,
    var comment: BaseModel? = null,
): BaseModel() {
    override fun copy(): Resource? = MeowLogicalModel(patient, medicationTreatmentLine, verification, comment)
    override fun getResourceType(): ResourceType? = ResourceType.StructureDefinition
    override fun fhirType(): String = "MedicationOverviewLM"

    override fun makeProperty(hash: Int, name: String?): Base {
        return when (hash) {
            "patient".hashCode() -> MeowPatientLM().also { patient = it }
            "medicationTreatmentLine".hashCode() -> MeowTreatmentLineLM().also { medicationTreatmentLine.add(it) }
            else -> super.makeProperty(hash, name)
        }
    }

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        when (hash) {
            "patient".hashCode() -> patient = (value as? MeowPatientLM)
            "medicationTreatmentLine".hashCode() -> (value as? MeowTreatmentLineLM)?.let { medicationTreatmentLine.add(it) }
            else -> return super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? = setProperty(name.hashCode(), name, value)
}

class MeowPatientLM(
    var name: HumanName? = null,
    var dateOfBirth: DateType? = null,
    var gender: CodeableConcept? = null,
    var identifier: Identifier? = null,
): BaseModel() {
    override fun fhirType(): String = "PatientLM"

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        when (hash) {
            "name".hashCode() -> this.name = (value as? HumanName)
            "dateOfBirth".hashCode() -> dateOfBirth = when (value) {
                is DateType -> value
                is DateTimeType -> DateType(value.valueAsString)
                else -> null
            }
            "gender".hashCode() -> gender = (value as? CodeableConcept)
            "identifier".hashCode() -> identifier = (value as? Identifier)
            else -> return super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? = setProperty(name.hashCode(), name, value)
}

class MeowTreatmentLineLM(
    var status: CodeType? = null,
    var medication: CodeableConcept? = null,
    var effectivePeriod: Period? = null,
    var recordingMetadata: MeowRecordingMetadata? = null,
    var preparationInstructions: StringType? = null,
    var usageInstructions: BaseModel? = null,
    var indication: CodeableConcept? = null,
    var indicationText: StringType? = null,
    var treatmentStatus: CodeableConcept? = null,
    var treatmentStatusReasonCode: CodeableConcept? = null,
    var identifier: Identifier? = null,
    var category: CodeableConcept? = null,
    var derivedFrom: BaseModel? = null,
    var version: StringType? = null,
    var verificationInformation: BaseModel? = null,
    var substitution: BaseModel? = null,
    var comment: BaseModel? = null,
): BaseModel() {
    override fun fhirType(): String = "MedicationTreatmentLineLM"

    override fun makeProperty(hash: Int, name: String?): Base {
        return when (hash) {
            "recordingMetadata".hashCode() -> MeowRecordingMetadata().also { recordingMetadata = it }
            else -> super.makeProperty(hash, name)
        }
    }

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        when (hash) {
            "status".hashCode() -> status = value.asCode()
            "medication".hashCode() -> medication = (value as? CodeableConcept)
            "effectivePeriod".hashCode() -> effectivePeriod = (value as? Period)
            "recordingMetadata".hashCode() -> recordingMetadata = (value as? MeowRecordingMetadata)
            "preparationInstructions".hashCode() -> preparationInstructions = (value as? StringType)
            "indicationText".hashCode() -> indicationText = (value as? StringType)
            "treatmentStatus".hashCode() -> treatmentStatus = (value as? CodeableConcept)
            "treatmentStatusReasonCode".hashCode() -> treatmentStatusReasonCode = (value as? CodeableConcept)
            "identifier".hashCode() -> identifier = (value as? Identifier)
            "category".hashCode() -> category = (value as? CodeableConcept)
            "version".hashCode() -> version = (value as? StringType)
            else -> return super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? = setProperty(name.hashCode(), name, value)

    private fun Base?.asCode(): CodeType? = when (this) {
        is CodeType -> this
        is StringType -> CodeType(value)
        else -> null
    }
}

class MeowRecordingMetadata(
    var recordedTime: DateTimeType? = null,
    var recorder: Reference? = null,
): BaseModel() {
    override fun fhirType(): String = "RecordingMetadata"

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        when (hash) {
            "recordedTime".hashCode() -> recordedTime = when (value) {
                is DateTimeType -> value
                is DateType -> DateTimeType(value.valueAsString)
                else -> null
            }
            "recorder".hashCode() -> recorder = (value as? Reference)
            else -> return super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? = setProperty(name.hashCode(), name, value)
}
