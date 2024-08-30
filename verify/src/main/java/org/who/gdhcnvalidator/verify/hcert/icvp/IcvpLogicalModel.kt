package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel

open class IcvpLogicalModel(
    name: StringType?,
    dob: DateType?,
    sex: Coding?,
    nationality: Coding?,

    nid: StringType?,
    guardian: StringType?,

    vaccineDetails: List<DvcVaccineDetails>
): DvcLogicalModel(name, dob, sex, nationality, nid, guardian, vaccineDetails)