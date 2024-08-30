package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel

class IcvpVaccineDetails (
    doseNumber: CodeableConcept,
    disease: Coding?,

    vaccineClassification: CodeableConcept,
    vaccineTradeItem: StringType?,
    date: DateTimeType?,

    clinicianName: StringType?,
    issuer: Reference?,

    manufacturerId: Identifier?,
    manufacturer: StringType?,

    batchNo: StringType?,
    validityPeriod: Period?,
): DvcVaccineDetails(doseNumber, disease, vaccineClassification, vaccineTradeItem, date, clinicianName, issuer, manufacturerId, manufacturer, batchNo, validityPeriod)