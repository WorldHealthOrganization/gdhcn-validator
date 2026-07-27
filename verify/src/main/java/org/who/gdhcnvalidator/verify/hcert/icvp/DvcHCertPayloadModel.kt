package org.who.gdhcnvalidator.verify.hcert.dcc.logical

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.api.client.util.DateTime
import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.ddcc.IdentifierDeserializer


class HCertDVC(
    val n: StringType?,       // Person name
    val dob: DateType?,       // Date of birth in YYYY-MM-DD format
    val s: CodeType?,           // Sex
    val ntl: CodeType?,         // Nationality
    val nid: StringType?,     // National Identification Document
    val ndt: CodeType?,       // National ID Document Type
    val gn: StringType?,      // Parent or Guardian Name

    val v: DvcHCertVaccination?, // Vaccination Group (Can only have one)
): BaseModel()

class DvcHCertVaccination(
    //val `@id`:StringType?,   // Unique id for inter-element referencing
    val extension: List<Extension>?, // Additional content defined by implementations
    val modifierExtension: List<Extension>?, // Extensions that cannot be ignored even if unrecognized

    val dn: CodeType?,     // Dose Number
    val tg:	CodeType?,     // Name of disease or condition vaccinated or received prophylaxis against
    val vp:	CodeType?,     // Vaccine or prophylaxis classification code
    val mp:	IdType?, // Vaccine Trade item id
    val ma:	StringType?, // manufacturer name
    val mid: IdType?, // manufacturer id
    val dt: DateType?,   // Date of vaccination, YYYY-MM-DD format
    val bo: StringType?, // batch number
    val vls: DateTime?,  // Certificate Validity periods start date
    val vle: DateTime?,  // Certificate Validity periods end date
    val cn: StringType?, // Name of supervising clinician
    val `is`:StringType?,// Certificate issuer (organization name)
): BaseModel()
