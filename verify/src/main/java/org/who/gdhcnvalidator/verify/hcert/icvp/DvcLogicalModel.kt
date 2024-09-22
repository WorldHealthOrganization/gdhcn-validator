package org.who.gdhcnvalidator.verify.hcert.icvp

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.ddcc.ReferenceDeserializer

open class DvcLogicalModel(
    var name: StringType? = null,
    var dob: DateType? = null,
    var sex: Coding? = null,
    var nationality: Coding? = null,

    var nid: StringType? = null,
    var guardian: StringType? = null,

    @JsonDeserialize(using = ReferenceDeserializer::class)
    var issuer: Reference? = null,
    var vaccineDetails: DvcVaccineDetails? = null,
): BaseModel() {
    override fun copy(): Resource? { return DvcLogicalModel(name, dob, sex, nationality, nid, guardian, issuer, vaccineDetails) }
    override fun getResourceType(): ResourceType? {
        println("DvcLogicalModel GetResourceType")
        return ResourceType.StructureDefinition
    }

    override fun makeProperty(hash: Int, name: String?): Base {
        println("DvcLogicalModel makeProperty")
        return when (hash) {
            "vaccineDetails".hashCode() -> {
                val newVac = DvcVaccineDetails()
                vaccineDetails = newVac
                newVac
            }
            else -> super.makeProperty(hash, name)
        }
    }

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        println("DvcLogicalModel setProperty $hash $name $value")
        when (hash) {
            "name".hashCode() -> this.name = (value as? StringType)
            "dob".hashCode() -> dob = (value as? DateType)
            "sex".hashCode() -> sex = (value as? Coding)
            "nationality".hashCode() -> nationality = (value as? Coding)
            "nid".hashCode() -> nid = (value as? StringType)
            "guardian".hashCode() -> guardian = (value as? StringType)
            "vaccineDetails".hashCode() -> vaccineDetails = (value as? DvcVaccineDetails)
            else -> super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? {
        println("DvcLogicalModel setProperty $name $value")
        when (name) {
            "name" -> this.name = (value as? StringType)
            "dob" -> dob = (value as? DateType)
            "sex" -> sex = (value as? Coding)
            "nationality" -> nationality = (value as? Coding)
            "nid" -> nid = (value as? StringType)
            "guardian" -> guardian = (value as? StringType)
            "vaccineDetails" -> vaccineDetails = (value as? DvcVaccineDetails)
            else -> super.setProperty(name, value)
        }
        return value
    }

    override fun fhirType(): String {
        return "ModelDVC"
    }
}