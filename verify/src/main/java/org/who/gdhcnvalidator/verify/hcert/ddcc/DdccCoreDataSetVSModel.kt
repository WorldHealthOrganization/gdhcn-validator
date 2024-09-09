package org.who.gdhcnvalidator.verify.hcert.dcc.logical

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.ddcc.DdccCertificate
import org.who.gdhcnvalidator.verify.hcert.ddcc.DdccCoreDataSet
import org.who.gdhcnvalidator.verify.hcert.ddcc.IdentifierDeserializer
import org.who.gdhcnvalidator.verify.hcert.ddcc.MyMeta
import org.who.gdhcnvalidator.verify.shc.DecimalToDataTimeDeserializer
import kotlin.reflect.full.declaredMemberProperties


// from https://build.fhir.org/ig/WorldHealthOrganization/ddcc/StructureDefinition-DDCCCoreDataSetVS.html
class DdccCoreDataSetVS (
    meta: MyMeta?,
    name: StringType?,
    birthDate: DateType?,
    sex: Coding?,
    @JsonDeserialize(using = IdentifierDeserializer::class)
    identifier: Identifier?,

    certificate: DdccCertificate?,

    // test
    val vaccination: Vaccination,
): DdccCoreDataSet(meta, name, birthDate, sex, identifier, certificate)

class DdccVaccination (
    val vaccine: Coding?,           // 1..1	Coding	Vaccine or prophylaxis
    val brand: Coding?,             // 1..1	Coding	Vaccine brand
    @JsonDeserialize(using = CodingOrReferenceDeserializer::class)
    val manufacturer: Base?,        // 0..1	Coding	Vaccine manufacturer
    val maholder: Coding?,          // 0..1	Coding	Vaccine market authorization holder
    val lot: StringType?,           // 1..1	string	Vaccine lot number
    val date: DateTimeType?,        // 1..1	dateTime	Date of vaccination
    val validFrom: DateTimeType?,   // 0..1	date	Vaccination valid from
    val dose: PositiveIntType?,     // 1..1	positiveInt	Dose number
    val total_doses: PositiveIntType?, // 0..1	positiveInt	Total doses
    val country: Coding?,           // 1..1	Coding	Country of vaccination
    val centre: StringType?,        // 0..1	string	Administering centre
    val signature: Signature?,      // 0..1	Signature	Signature of health worker

    @JsonDeserialize(using = IdentifierDeserializer::class)
    val practitioner: Identifier?,  // 0..1	Identifier	Health worker identifier
    val disease: Coding?,           // 0..1	Coding	Disease or agent targeted
    val nextDose: DateTimeType?,    // 0..1	date	Due date of next dose
): BaseModel()


// HACK because of an invalid QR submitted
object CodingOrReferenceDeserializer: JsonDeserializer<Base>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Base? {
        val token: TreeNode = p.readValueAsTree()

        return if (token.isValueNode) {
            Reference().apply {
                id = token.toString()
            }
        } else {
            return jacksonObjectMapper().readValue<Coding>(token.toString())
        }
    }
}