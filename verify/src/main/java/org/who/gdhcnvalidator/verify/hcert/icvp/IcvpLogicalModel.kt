package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*

open class IcvpLogicalModel(
    name: StringType?,
    dob: DateType?,
    sex: Coding?,
    nationality: Coding?,

    nid: StringType?,
    guardian: StringType?,

    issuer: Reference?,
    vaccineDetails: DvcVaccineDetails
): DvcLogicalModel(name, dob, sex, nationality, nid, guardian, issuer, vaccineDetails)