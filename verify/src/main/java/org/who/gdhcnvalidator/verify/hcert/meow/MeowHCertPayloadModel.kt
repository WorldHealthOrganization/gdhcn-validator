package org.who.gdhcnvalidator.verify.hcert.meow

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel

/**
 * MeOW (Medication Overview) HCERT claim (-7) payload, per
 * http://smart.who.int/ph4h/StructureDefinition/MedicationOverviewMin
 */
class HCertMeow(
    val n: StringType?,   // Patient Name
    val dob: DateType?,   // Patient Date of Birth
    val s: CodeType?,     // Sex
    val nt: CodeType?,    // Nationality
    val id: StringType?,  // National Identifier
    val dt: CodeType?,    // National Identifier Type
    val m: List<MeowTreatmentLineMin>?, // Medication Treatment Lines
): BaseModel()

class MeowTreatmentLineMin(
    val m: CodeType?,      // Medication code (SNOMED CT)
    val r: StringType?,    // Reason / indication text
    val d: StringType?,    // Dosage / preparation instructions
    // dateTime types so the StructureMap engine can copy them into Period/dateTime elements
    val da: DateTimeType?, // Date asserted
    val es: DateTimeType?, // Effective period start
    val ee: DateTimeType?, // Effective period end
    val a: CodeType?,      // Adherence / treatment status
): BaseModel()
