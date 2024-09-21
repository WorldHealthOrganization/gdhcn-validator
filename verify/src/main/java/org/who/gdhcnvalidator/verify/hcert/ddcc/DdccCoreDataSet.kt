package org.who.gdhcnvalidator.verify.hcert.ddcc

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.who.gdhcnvalidator.verify.BaseModel
import kotlin.reflect.full.declaredMemberProperties

open class DdccCoreDataSet (
    val meta: MyMeta?,

    val name: StringType?,
    val birthDate: DateType?,
    val sex: Coding?,
    @JsonDeserialize(using = IdentifierDeserializer::class)
    val identifier: Identifier?,

    val certificate: DdccCertificate?
): BaseModel()

class DdccCertificate(
    @JsonDeserialize(using = ReferenceDeserializer::class)
    val issuer: Reference?,	    //1..1	Reference(DDCC Organization)	Certificate issuer
    val kid: StringType?,		//0..1	string	Key ID used to verify the signature of the certificate
    @JsonDeserialize(using = IdentifierDeserializer::class)
    val hcid: Identifier?,	    //1..1	Identifier	Health certificate identifier (HCID)
    @JsonDeserialize(using = IdentifierDeserializer::class)
    val ddccid: Identifier?,	//0..1	Identifier	DDCC Identifier
    val version: StringType?,	//1..1	string	Certificate schema version
    val period: Period,	        //0..1	Period	Certificate Validity Period
): BaseModel()


class MyMeta (
    val notarisedOn: DateTimeType?,
    val reference: StringType?,
    val url: StringType?,
    val passportNumber: StringType?
): org.hl7.fhir.r4.model.Meta() {
    private val propertiesByHash = this::class.declaredMemberProperties.associateBy { it.name.hashCode() }

    override fun getProperty(hash: Int, name: String?, checkValid: Boolean): Array<Base?> {
        return propertiesByHash[hash]?.let {
            val prop = it.getter.call(this)
            if (prop == null) {
                emptyArray()
            } else if (prop is Base) {
                arrayOf(prop)
            } else if (prop is Collection<*>) {
                if (prop.isEmpty()) {
                    emptyArray()
                } else {
                    (prop as Collection<Base?>).toTypedArray()
                }
            } else {
                emptyArray()
            }
        } ?: super.getProperty(hash, name, checkValid)
    }
}

object ReferenceDeserializer: JsonDeserializer<Reference>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Reference? {
        val token: TreeNode = p.readValueAsTree()

        println("ReferenceDeserializer" + token.toString())

        return if (token.isValueNode) {
            Reference().apply {
                id = token.toString()
            }
        } else {
            return jacksonObjectMapper().readValue<Reference>(token.toString())
        }
    }
}

object IdentifierDeserializer: JsonDeserializer<Base>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Base? {
        val token: TreeNode = p.readValueAsTree()

        println("IdentifierDeserializer" + token.toString())

        return if (token.isValueNode) {
            Identifier().apply {
                id = token.toString()
            }
        } else {
            return null
        }
    }
}