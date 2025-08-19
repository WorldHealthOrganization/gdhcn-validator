package org.who.gdhcnvalidator.verify.hcert.healthlink

import org.hl7.fhir.r4.model.StringType
import org.who.gdhcnvalidator.verify.BaseModel

open class SmartHealthLinkModel (
    val u: StringType?
): BaseModel()