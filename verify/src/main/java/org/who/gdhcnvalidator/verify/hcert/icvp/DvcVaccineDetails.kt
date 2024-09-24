package org.who.gdhcnvalidator.verify.hcert.icvp

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.ddcc.IdentifierDeserializer
import org.who.gdhcnvalidator.verify.hcert.ddcc.ReferenceDeserializer

open class DvcVaccineDetails (
    var doseNumber: CodeableConcept? = null,
    var disease: Coding? = null,

    var vaccineClassification: CodeableConcept? = null,
    var vaccineTradeItem: StringType? = null,
    var date: DateTimeType? = null,

    var clinicianName: StringType? = null,

    @JsonDeserialize(using = ReferenceDeserializer::class)
    var issuer: Reference? = null,

    @JsonDeserialize(using = IdentifierDeserializer::class)
    var manufacturerId: Identifier? = null,
    var manufacturer: StringType? = null,

    var batchNo: StringType? = null,
    var validityPeriod: Period? = null,
): BaseModel() {
    override fun makeProperty(hash: Int, name: String?): Base {
        println("DvcVaccineDetails issuer")
        return when (hash) {
            "issuer".hashCode() -> {
                val newRef = Reference()
                issuer = newRef
                newRef
            }
            else -> super.makeProperty(hash, name)
        }
    }

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        println("DvcVaccineDetails setProperty $hash $name $value")
        when (hash) {
            "doseNumber".hashCode() -> doseNumber = (value as? CodeableConcept)
            "disease".hashCode() -> disease = (value as? Coding)
            "vaccineClassification".hashCode() -> vaccineClassification = (value as? CodeableConcept)
            "vaccineTradeItem".hashCode() -> vaccineTradeItem = (value as? StringType)
            "date".hashCode() -> date = (value as? DateTimeType)
            "clinicianName".hashCode() -> clinicianName = (value as? StringType)
            "issuer".hashCode() -> issuer = (value as? Reference)
            "manufacturerId".hashCode() -> manufacturerId = (value as? Identifier)
            "manufacturer".hashCode() -> manufacturer = (value as? StringType)
            "batchNo".hashCode() -> batchNo = (value as? StringType)
            "validityPeriod".hashCode() -> validityPeriod = (value as? Period)
            else -> super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? {
        println("DvcVaccineDetails setProperty $name $value")
        when (name) {
            "doseNumber" -> doseNumber = (value as? CodeableConcept)
            "disease" -> disease = (value as? Coding)
            "vaccineClassification" -> vaccineClassification = (value as? CodeableConcept)
            "vaccineTradeItem" -> vaccineTradeItem = (value as? StringType)
            "date" -> date = (value as? DateTimeType)
            "clinicianName" -> clinicianName = (value as? StringType)
            "issuer" -> issuer = (value as? Reference)
            "manufacturerId" -> manufacturerId = (value as? Identifier)
            "manufacturer" -> manufacturer = (value as? StringType)
            "batchNo" -> batchNo = (value as? StringType)
            "validityPeriod" -> validityPeriod = (value as? Period)
            else -> super.setProperty(name, value)
        }
        return value
    }
}