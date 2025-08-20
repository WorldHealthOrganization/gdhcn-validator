package org.who.gdhcnvalidator.ipsviewer

import java.time.LocalDate

/**
 * Structured data models for FHIR International Patient Summary content
 */

/**
 * Complete IPS document representation
 */
data class IpsDocument(
    val patient: IpsPatient,
    val composition: IpsComposition?,
    val medications: List<IpsMedication> = emptyList(),
    val allergies: List<IpsAllergy> = emptyList(),
    val conditions: List<IpsCondition> = emptyList(),
    val immunizations: List<IpsImmunization> = emptyList(),
    val procedures: List<IpsProcedure> = emptyList(),
    val observations: List<IpsObservation> = emptyList(),
    val diagnosticReports: List<IpsDiagnosticReport> = emptyList(),
    val otherResources: Map<String, Int> = emptyMap()
)

/**
 * Patient demographic information
 */
data class IpsPatient(
    val name: String?,
    val givenNames: List<String> = emptyList(),
    val familyName: String?,
    val birthDate: LocalDate?,
    val gender: String?,
    val identifiers: List<IpsIdentifier> = emptyList(),
    val telecom: List<IpsContactPoint> = emptyList(),
    val addresses: List<IpsAddress> = emptyList()
)

/**
 * Document composition metadata
 */
data class IpsComposition(
    val title: String?,
    val date: String?,
    val author: String?,
    val sections: List<IpsSection> = emptyList()
)

/**
 * IPS document section
 */
data class IpsSection(
    val title: String?,
    val code: String?,
    val text: String?,
    val resourceCount: Int = 0
)

/**
 * Medication information
 */
data class IpsMedication(
    val name: String?,
    val status: String?,
    val dosage: String?,
    val route: String?,
    val frequency: String?,
    val effectiveDate: String?,
    val note: String?
)

/**
 * Allergy/Intolerance information
 */
data class IpsAllergy(
    val substance: String?,
    val category: String?,
    val criticality: String?,
    val type: String?,
    val clinicalStatus: String?,
    val verificationStatus: String?,
    val reactions: List<IpsReaction> = emptyList(),
    val onset: String?,
    val note: String?
)

/**
 * Allergic reaction details
 */
data class IpsReaction(
    val manifestation: String?,
    val severity: String?,
    val note: String?
)

/**
 * Medical condition/problem
 */
data class IpsCondition(
    val name: String?,
    val category: String?,
    val severity: String?,
    val clinicalStatus: String?,
    val verificationStatus: String?,
    val onsetDate: String?,
    val abatementDate: String?,
    val note: String?
)

/**
 * Immunization record
 */
data class IpsImmunization(
    val vaccineCode: String?,
    val vaccineName: String?,
    val status: String?,
    val occurrenceDate: String?,
    val doseNumber: Int?,
    val seriesDoses: Int?,
    val lotNumber: String?,
    val manufacturer: String?,
    val site: String?,
    val route: String?,
    val performer: String?,
    val note: String?
)

/**
 * Medical procedure
 */
data class IpsProcedure(
    val name: String?,
    val status: String?,
    val category: String?,
    val performedDate: String?,
    val performer: String?,
    val bodySite: String?,
    val outcome: String?,
    val note: String?
)

/**
 * Clinical observation
 */
data class IpsObservation(
    val name: String?,
    val category: String?,
    val status: String?,
    val value: String?,
    val unit: String?,
    val interpretation: String?,
    val effectiveDate: String?,
    val note: String?,
    val referenceRange: String?
)

/**
 * Diagnostic report
 */
data class IpsDiagnosticReport(
    val name: String?,
    val category: String?,
    val status: String?,
    val effectiveDate: String?,
    val issued: String?,
    val performer: String?,
    val conclusion: String?,
    val presentedForm: String?
)

/**
 * Patient identifier
 */
data class IpsIdentifier(
    val system: String?,
    val value: String?,
    val type: String?,
    val use: String?
)

/**
 * Contact point (phone, email, etc.)
 */
data class IpsContactPoint(
    val system: String?,
    val value: String?,
    val use: String?,
    val rank: Int?
)

/**
 * Address information
 */
data class IpsAddress(
    val use: String?,
    val type: String?,
    val text: String?,
    val line: List<String> = emptyList(),
    val city: String?,
    val district: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
)