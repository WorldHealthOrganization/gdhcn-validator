package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel

open class DvcVaccineDetails (
    val doseNumber: CodeableConcept,
    val disease: Coding?,

    val vaccineClassification: CodeableConcept,
    val vaccineTradeItem: StringType?,
    val date: DateTimeType?,

    val clinicianName: StringType?,
    val issuer: Reference?,

    val manufacturerId: Identifier?,
    val manufacturer: StringType?,

    val batchNo: StringType?,
    val validityPeriod: Period?,
): BaseModel()