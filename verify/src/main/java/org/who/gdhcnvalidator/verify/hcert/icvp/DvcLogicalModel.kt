package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel

open class DvcLogicalModel(
    val name: StringType?,
    val dob: DateType?,
    val sex: Coding?,
    val nationality: Coding?,

    val nid: StringType?,
    val guardian: StringType?,

    val vaccineDetails: List<DvcVaccineDetails>
): BaseModel()