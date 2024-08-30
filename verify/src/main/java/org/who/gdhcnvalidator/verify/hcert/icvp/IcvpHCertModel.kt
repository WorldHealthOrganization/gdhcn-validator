package org.who.gdhcnvalidator.verify.hcert.dcc.logical

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.shc.DecimalToDataTimeDeserializer
import java.util.Date


class IcvpCwt (
    @JsonProperty("1")
    val iss: StringType?,   // Issuer
    @JsonProperty("2")
    val sub: StringType?,   // Subject
    @JsonProperty("3")
    val aud: StringType?,   // Audience
    @JsonProperty("4")
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val exp: DateTimeType?, // expiration
    @JsonProperty("5")
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val nbf: DateTimeType?, // not before date
    @JsonProperty("6")
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val iat: DateTimeType?, // issued at date
    @JsonProperty("7")
    val id: StringType?,   // Audience
    @JsonProperty("-260")
    val data: IcvpHCert?,      // Certificate
): BaseModel()

class IcvpHCert(
    @JsonProperty("1")
    val cert: HC1?          // Cert
): BaseModel()

class DvcHCert(
    val n: StringType?,       // Person name
    val dob: DateType?,       // Date of birth in YYYY-MM-DD format
    val s: Coding?,           // Sex
    val ntl: Coding?,         // Nationality
    val nid: StringType?,     // National Identification Document
    val gn: StringType?,      // Parent or Guardian Name

    val v: List<DvcHCertVaccination>?,  // Vaccination Group
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
