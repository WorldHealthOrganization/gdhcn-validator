package org.who.gdhcnvalidator.verify.hcert.dcc.logical

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel


/**
 * ICVP HCERT claim (-6) payload, per
 * http://smart.who.int/icvp/StructureDefinition/ICVPMin
 */
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

    val dn: CodeType?,     // Dose Number (legacy DVCMin)
    val tg:	CodeType?,     // Disease or condition vaccinated against (legacy DVCMin)
    val vp:	CodeType?,     // Vaccine or prophylaxis PreQual product id
    val mp:	IdType?, // Vaccine Trade item id (legacy DVCMin)
    val ma:	StringType?, // manufacturer name (legacy DVCMin)
    val mid: IdType?, // manufacturer id (legacy DVCMin)
    val dt: DateType?,   // Date of vaccination, YYYY-MM-DD format
    val bo: StringType?, // batch number
    // dateTime types so the StructureMap engine can copy them into Period.start/end
    val vls: DateTimeType?,  // Certificate Validity periods start date
    val vle: DateTimeType?,  // Certificate Validity periods end date
    val cn: StringType?, // Name of supervising clinician
    val `is`:StringType?,// Certificate issuer (organization name)
): BaseModel()
