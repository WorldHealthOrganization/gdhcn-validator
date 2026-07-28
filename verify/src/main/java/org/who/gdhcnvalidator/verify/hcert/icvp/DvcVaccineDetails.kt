package org.who.gdhcnvalidator.verify.hcert.icvp

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.ddcc.IdentifierDeserializer
import org.who.gdhcnvalidator.verify.hcert.ddcc.ReferenceDeserializer

/**
 * Logical model per http://smart.who.int/icvp/StructureDefinition/ICVPVaccineDetails
 * (legacy DVC fields kept for older payloads)
 */
open class DvcVaccineDetails (
    var doseNumber: CodeableConcept? = null,
    var disease: Coding? = null,

    var vaccineClassification: CodeableConcept? = null,
    var vaccineTradeItem: StringType? = null,
    var date: DateType? = null,

    var clinicianName: StringType? = null,

    @JsonDeserialize(using = ReferenceDeserializer::class)
    var issuer: Reference? = null,

    @JsonDeserialize(using = IdentifierDeserializer::class)
    var manufacturerId: Identifier? = null,
    var manufacturer: StringType? = null,

    var batchNo: CodeableConcept? = null,
    var validityPeriod: Period? = null,
    var productID: Coding? = null,
): BaseModel() {
    override fun makeProperty(hash: Int, name: String?): Base {
        return when (hash) {
            "issuer".hashCode() -> {
                val newRef = Reference()
                issuer = newRef
                newRef
            }
            else -> super.makeProperty(hash, name)
        }
    }

    private fun asDate(value: Base?): DateType? = when (value) {
        is DateType -> value
        is DateTimeType -> DateType(value.valueAsString)
        else -> null
    }

    override fun setProperty(hash: Int, name: String?, value: Base?): Base? {
        when (hash) {
            "doseNumber".hashCode() -> doseNumber = (value as? CodeableConcept)
            "disease".hashCode() -> disease = (value as? Coding)
            "vaccineClassification".hashCode() -> vaccineClassification = (value as? CodeableConcept)
            "vaccineTradeItem".hashCode() -> vaccineTradeItem = (value as? StringType)
            "date".hashCode() -> date = asDate(value)
            "clinicianName".hashCode() -> clinicianName = (value as? StringType)
            "issuer".hashCode() -> issuer = (value as? Reference)
            "manufacturerId".hashCode() -> manufacturerId = (value as? Identifier)
            "manufacturer".hashCode() -> manufacturer = (value as? StringType)
            "batchNo".hashCode() -> batchNo = (value as? CodeableConcept)
            "validityPeriod".hashCode() -> validityPeriod = (value as? Period)
            "productID".hashCode() -> productID = (value as? Coding)
            else -> super.setProperty(hash, name, value)
        }
        return value
    }

    override fun setProperty(name: String?, value: Base?): Base? {
        when (name) {
            "doseNumber" -> doseNumber = (value as? CodeableConcept)
            "disease" -> disease = (value as? Coding)
            "vaccineClassification" -> vaccineClassification = (value as? CodeableConcept)
            "vaccineTradeItem" -> vaccineTradeItem = (value as? StringType)
            "date" -> date = asDate(value)
            "clinicianName" -> clinicianName = (value as? StringType)
            "issuer" -> issuer = (value as? Reference)
            "manufacturerId" -> manufacturerId = (value as? Identifier)
            "manufacturer" -> manufacturer = (value as? StringType)
            "batchNo" -> batchNo = (value as? CodeableConcept)
            "validityPeriod" -> validityPeriod = (value as? Period)
            "productID" -> productID = (value as? Coding)
            else -> super.setProperty(name, value)
        }
        return value
    }

    override fun fhirType(): String {
        return "ICVPVaccineDetails"
    }

    /**
     * Validates ICVP constraints for vaccine details
     */
    open fun validateIcvpConstraints(): List<String> {
        val errors = mutableListOf<String>()

        // Validate must-have-issuer-or-clinician-name invariant
        if (!IcvpValidation.validateIssuerOrClinicanName(
            issuer?.reference?.let { StringType(it) },
            clinicianName)) {
            errors.add("Either issuer or clinicianName must be present")
        }

        return errors
    }
}
