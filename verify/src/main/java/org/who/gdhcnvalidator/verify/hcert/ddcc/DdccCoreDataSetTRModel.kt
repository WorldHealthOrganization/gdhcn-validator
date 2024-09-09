package org.who.gdhcnvalidator.verify.hcert.dcc.logical

import org.hl7.fhir.r4.model.*
import org.who.gdhcnvalidator.verify.BaseModel
import org.who.gdhcnvalidator.verify.hcert.ddcc.DdccCertificate
import org.who.gdhcnvalidator.verify.hcert.ddcc.DdccCoreDataSet
import org.who.gdhcnvalidator.verify.hcert.ddcc.MyMeta

class DdccCoreDataSetTR (
    meta: MyMeta?,
    name: StringType?,
    birthDate: DateType?,
    sex: Coding?,
    identifier: Identifier?,

    certificate: DdccCertificate?,

    // test
    val test: DdccTestResult,
): DdccCoreDataSet(meta, name, birthDate, sex, identifier, certificate)

class DdccTestResult (
    val pathogen: Coding?,
    val type: Coding?,
    val brand: Coding?,
    val manufacturer: Coding?,
    val origin: Coding?,
    val date: DateTimeType?,
    val result: Coding?,
    val centre: StringType?,
    val country: Coding?
): BaseModel()