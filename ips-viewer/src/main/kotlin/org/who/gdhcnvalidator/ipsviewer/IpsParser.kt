package org.who.gdhcnvalidator.ipsviewer

import org.hl7.fhir.r4.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parser for FHIR International Patient Summary bundles
 * Extracts structured data from IPS Bundle resources
 */
class IpsParser {

    companion object {
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-MM"),
            DateTimeFormatter.ofPattern("yyyy")
        )
    }

    /**
     * Parse a FHIR Bundle into structured IPS data
     */
    fun parseIpsBundle(bundle: Bundle): IpsDocument {
        val entries = bundle.entry ?: emptyList()
        
        // Extract patient (required for IPS)
        val patientResource = entries.find { it.resource is Patient }?.resource as? Patient
        val patient = patientResource?.let { parsePatient(it) } 
            ?: throw IllegalArgumentException("IPS Bundle must contain a Patient resource")

        // Extract composition
        val compositionResource = entries.find { it.resource is Composition }?.resource as? Composition
        val composition = compositionResource?.let { parseComposition(it) }

        // Extract clinical resources
        val medications = entries.filter { it.resource is MedicationStatement || it.resource is Medication }
            .mapNotNull { parseMedication(it.resource) }

        val allergies = entries.filter { it.resource is AllergyIntolerance }
            .mapNotNull { parseAllergy(it.resource as AllergyIntolerance) }

        val conditions = entries.filter { it.resource is Condition }
            .mapNotNull { parseCondition(it.resource as Condition) }

        val immunizations = entries.filter { it.resource is Immunization }
            .mapNotNull { parseImmunization(it.resource as Immunization) }

        val procedures = entries.filter { it.resource is Procedure }
            .mapNotNull { parseProcedure(it.resource as Procedure) }

        val observations = entries.filter { it.resource is Observation }
            .mapNotNull { parseObservation(it.resource as Observation) }

        val diagnosticReports = entries.filter { it.resource is DiagnosticReport }
            .mapNotNull { parseDiagnosticReport(it.resource as DiagnosticReport) }

        // Count other resource types
        val handledTypes = setOf("Patient", "Composition", "MedicationStatement", "Medication",
            "AllergyIntolerance", "Condition", "Immunization", "Procedure", "Observation", "DiagnosticReport")
        
        val otherResources = entries.groupBy { it.resource?.fhirType() }
            .filterKeys { it != null && !handledTypes.contains(it) }
            .mapValues { it.value.size }
            .mapKeys { it.key!! }

        return IpsDocument(
            patient = patient,
            composition = composition,
            medications = medications,
            allergies = allergies,
            conditions = conditions,
            immunizations = immunizations,
            procedures = procedures,
            observations = observations,
            diagnosticReports = diagnosticReports,
            otherResources = otherResources
        )
    }

    private fun parsePatient(patient: Patient): IpsPatient {
        val name = patient.name?.firstOrNull()
        val givenNames = name?.given?.map { it.value } ?: emptyList()
        val familyName = name?.family
        val fullName = (givenNames + listOfNotNull(familyName)).joinToString(" ").takeIf { it.isNotBlank() }

        val birthDate = patient.birthDate?.let { parseDate(it.toString()) }
        val gender = patient.gender?.display ?: patient.gender?.name

        val identifiers = patient.identifier?.map { parseIdentifier(it) } ?: emptyList()
        val telecom = patient.telecom?.map { parseContactPoint(it) } ?: emptyList()
        val addresses = patient.address?.map { parseAddress(it) } ?: emptyList()

        return IpsPatient(
            name = fullName,
            givenNames = givenNames,
            familyName = familyName,
            birthDate = birthDate,
            gender = gender,
            identifiers = identifiers,
            telecom = telecom,
            addresses = addresses
        )
    }

    private fun parseComposition(composition: Composition): IpsComposition {
        val sections = composition.section?.map { section ->
            IpsSection(
                title = section.title,
                code = section.code?.coding?.firstOrNull()?.code,
                text = section.text?.div?.toString(),
                resourceCount = section.entry?.size ?: 0
            )
        } ?: emptyList()

        return IpsComposition(
            title = composition.title,
            date = composition.date?.toString(),
            author = composition.author?.firstOrNull()?.display,
            sections = sections
        )
    }

    private fun parseMedication(resource: Resource): IpsMedication? {
        return when (resource) {
            is MedicationStatement -> {
                val medication = resource.medicationCodeableConcept?.text 
                    ?: resource.medicationCodeableConcept?.coding?.firstOrNull()?.display

                IpsMedication(
                    name = medication,
                    status = resource.status?.display,
                    dosage = resource.dosage?.firstOrNull()?.text,
                    route = resource.dosage?.firstOrNull()?.route?.text,
                    frequency = resource.dosage?.firstOrNull()?.timing?.code?.text,
                    effectiveDate = resource.effective?.toString(),
                    note = resource.note?.firstOrNull()?.text
                )
            }
            is Medication -> {
                IpsMedication(
                    name = resource.code?.text ?: resource.code?.coding?.firstOrNull()?.display,
                    status = null,
                    dosage = null,
                    route = null,
                    frequency = null,
                    effectiveDate = null,
                    note = null
                )
            }
            else -> null
        }
    }

    private fun parseAllergy(allergy: AllergyIntolerance): IpsAllergy {
        val reactions = allergy.reaction?.map { reaction ->
            IpsReaction(
                manifestation = reaction.manifestation?.firstOrNull()?.text,
                severity = reaction.severity?.display,
                note = reaction.note?.firstOrNull()?.text
            )
        } ?: emptyList()

        return IpsAllergy(
            substance = allergy.code?.text ?: allergy.code?.coding?.firstOrNull()?.display,
            category = allergy.category?.firstOrNull()?.display,
            criticality = allergy.criticality?.display,
            type = allergy.type?.display,
            clinicalStatus = allergy.clinicalStatus?.coding?.firstOrNull()?.display,
            verificationStatus = allergy.verificationStatus?.coding?.firstOrNull()?.display,
            reactions = reactions,
            onset = allergy.onset?.toString(),
            note = allergy.note?.firstOrNull()?.text
        )
    }

    private fun parseCondition(condition: Condition): IpsCondition {
        return IpsCondition(
            name = condition.code?.text ?: condition.code?.coding?.firstOrNull()?.display,
            category = condition.category?.firstOrNull()?.coding?.firstOrNull()?.display,
            severity = condition.severity?.coding?.firstOrNull()?.display,
            clinicalStatus = condition.clinicalStatus?.coding?.firstOrNull()?.display,
            verificationStatus = condition.verificationStatus?.coding?.firstOrNull()?.display,
            onsetDate = condition.onset?.toString(),
            abatementDate = condition.abatement?.toString(),
            note = condition.note?.firstOrNull()?.text
        )
    }

    private fun parseImmunization(immunization: Immunization): IpsImmunization {
        return IpsImmunization(
            vaccineCode = immunization.vaccineCode?.coding?.firstOrNull()?.code,
            vaccineName = immunization.vaccineCode?.text ?: immunization.vaccineCode?.coding?.firstOrNull()?.display,
            status = immunization.status?.display,
            occurrenceDate = immunization.occurrence?.toString(),
            doseNumber = immunization.protocolApplied?.firstOrNull()?.doseNumber?.value?.toIntOrNull(),
            seriesDoses = immunization.protocolApplied?.firstOrNull()?.seriesDoses?.value?.toIntOrNull(),
            lotNumber = immunization.lotNumber,
            manufacturer = immunization.manufacturer?.display,
            site = immunization.site?.text,
            route = immunization.route?.text,
            performer = immunization.performer?.firstOrNull()?.actor?.display,
            note = immunization.note?.firstOrNull()?.text
        )
    }

    private fun parseProcedure(procedure: Procedure): IpsProcedure {
        return IpsProcedure(
            name = procedure.code?.text ?: procedure.code?.coding?.firstOrNull()?.display,
            status = procedure.status?.display,
            category = procedure.category?.text,
            performedDate = procedure.performed?.toString(),
            performer = procedure.performer?.firstOrNull()?.actor?.display,
            bodySite = procedure.bodySite?.firstOrNull()?.text,
            outcome = procedure.outcome?.text,
            note = procedure.note?.firstOrNull()?.text
        )
    }

    private fun parseObservation(observation: Observation): IpsObservation {
        val value = when {
            observation.hasValueQuantity() -> "${observation.valueQuantity.value} ${observation.valueQuantity.unit ?: observation.valueQuantity.code}"
            observation.hasValueString() -> observation.valueStringType.value
            observation.hasValueCodeableConcept() -> observation.valueCodeableConcept.text
            observation.hasValueBoolean() -> observation.valueBooleanType.value.toString()
            observation.hasValueInteger() -> observation.valueIntegerType.value.toString()
            observation.hasValueDateTime() -> observation.valueDateTimeType.value.toString()
            else -> null
        }

        return IpsObservation(
            name = observation.code?.text ?: observation.code?.coding?.firstOrNull()?.display,
            category = observation.category?.firstOrNull()?.coding?.firstOrNull()?.display,
            status = observation.status?.display,
            value = value,
            unit = observation.valueQuantity?.unit,
            interpretation = observation.interpretation?.firstOrNull()?.text,
            effectiveDate = observation.effective?.toString(),
            note = observation.note?.firstOrNull()?.text,
            referenceRange = observation.referenceRange?.firstOrNull()?.text?.div?.toString()
        )
    }

    private fun parseDiagnosticReport(report: DiagnosticReport): IpsDiagnosticReport {
        return IpsDiagnosticReport(
            name = report.code?.text ?: report.code?.coding?.firstOrNull()?.display,
            category = report.category?.firstOrNull()?.coding?.firstOrNull()?.display,
            status = report.status?.display,
            effectiveDate = report.effective?.toString(),
            issued = report.issued?.toString(),
            performer = report.performer?.firstOrNull()?.display,
            conclusion = report.conclusion,
            presentedForm = report.presentedForm?.firstOrNull()?.title
        )
    }

    private fun parseIdentifier(identifier: Identifier): IpsIdentifier {
        return IpsIdentifier(
            system = identifier.system,
            value = identifier.value,
            type = identifier.type?.text,
            use = identifier.use?.display
        )
    }

    private fun parseContactPoint(contact: ContactPoint): IpsContactPoint {
        return IpsContactPoint(
            system = contact.system?.display,
            value = contact.value,
            use = contact.use?.display,
            rank = contact.rank
        )
    }

    private fun parseAddress(address: Address): IpsAddress {
        return IpsAddress(
            use = address.use?.display,
            type = address.type?.display,
            text = address.text,
            line = address.line?.map { it.value } ?: emptyList(),
            city = address.city,
            district = address.district,
            state = address.state,
            postalCode = address.postalCode,
            country = address.country
        )
    }

    private fun parseDate(dateString: String): LocalDate? {
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString, formatter)
            } catch (e: DateTimeParseException) {
                // Try next formatter
            }
        }
        return null
    }
}