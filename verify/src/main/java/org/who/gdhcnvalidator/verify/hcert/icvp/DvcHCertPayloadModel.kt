package org.who.gdhcnvalidator.verify.hcert.dcc.logical

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel


class HCertDVC(
    val n: StringType?,       // Person name
    val dob: DateType?,       // Date of birth in YYYY-MM-DD format
    val s: Coding?,           // Sex
    val ntl: Coding?,         // Nationality
    val nid: StringType?,     // National Identification Document
    val gn: StringType?,      // Parent or Guardian Name

    val v: List<DvcHCertVaccination>?,  // Vaccination Group (Can only have one)
): BaseModel()

class DvcHCertVaccination(
    val tg:	Coding?,     // Name of disease or condition vaccinated or received prophylaxis against
    val vp:	Coding?,     // Vaccine or prophylaxis classification code
    val mp:	Identifier?, // Vaccine Trade item id
    val ma:	StringType?, // manufacturer name
    val mid:Identifier?, // manufacturer id
    val dt: DateType?,   // Date of vaccination, YYYY-MM-DD format
    val bo: StringType?, // batch number
    val dls: DateType?,  // Certificate Validity periods start date
    val dle: DateType?,  // Certificate Validity periods end date
    val cn: StringType?, // Name of supervising clinician
    val `is`:StringType?,// Certificate issuer (organization name)

    val extension: List<Extension>?, // Additional content defined by implementations
    val modifierExtension: List<Extension>? // Extensions that cannot be ignored even if unrecognized
): BaseModel()
