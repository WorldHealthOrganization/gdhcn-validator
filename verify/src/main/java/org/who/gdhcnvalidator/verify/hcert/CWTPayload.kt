package org.who.gdhcnvalidator.verify.hcert

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.ValueSet
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.DdccCoreDataSetTR
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.DdccCoreDataSetVS
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.HCertDCC
import org.who.gdhcnvalidator.verify.hcert.dcc.logical.HCertDVC
import java.util.Date


// from: https://worldhealthorganization.github.io/smart-trust/StructureDefinition-HCert.html
class CWTPayload (
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
    val data: HCert?,      // Certificate
): BaseModel()

class HCert(
    @JsonProperty("1")
    val dcc: HCertDCC?,
    @JsonProperty("3")
    val coreDataSetVS: DdccCoreDataSetVS?,
/*
    @JsonProperty("4")
    val coreDataSetTR: DdccCoreDataSetTR?,
    @JsonProperty("5")
    val smartHealthLink: Any?,
    @JsonProperty("-6")
    val dvc: HCertDVC?,
 */
): BaseModel()

object DecimalToDataTimeDeserializer: JsonDeserializer<DateTimeType>() {
    private fun parseDateType(date: Double?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(Date((date*1000).toLong()), TemporalPrecisionEnum.DAY)
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DateTimeType? {
        return parseDateType(p.decimalValue?.toDouble())
    }
}