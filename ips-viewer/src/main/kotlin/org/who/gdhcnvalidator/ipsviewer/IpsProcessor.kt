package org.who.gdhcnvalidator.ipsviewer

/**
 * Processor for IPS documents that applies business logic and organizes data
 */
class IpsProcessor {

    /**
     * Processes an IPS document and returns organized data with business rules applied
     */
    fun processIpsDocument(ipsDocument: IpsDocument): ProcessedIpsDocument {
        return ProcessedIpsDocument(
            patient = processPatientInfo(ipsDocument.patient),
            summary = createDocumentSummary(ipsDocument),
            sections = createSections(ipsDocument),
            alerts = generateAlerts(ipsDocument),
            metadata = extractMetadata(ipsDocument)
        )
    }

    private fun processPatientInfo(patient: IpsPatient): ProcessedPatientInfo {
        val displayName = patient.name ?: "Unknown Patient"
        val ageInfo = patient.birthDate?.let { birthDate ->
            val age = java.time.Period.between(birthDate, java.time.LocalDate.now()).years
            "Age $age"
        }

        val primaryIdentifier = patient.identifiers.find { it.use == "usual" || it.use == "official" }
            ?: patient.identifiers.firstOrNull()

        val contactInfo = patient.telecom.filter { it.system in listOf("phone", "email") }
            .sortedBy { if (it.system == "phone") 0 else 1 }

        return ProcessedPatientInfo(
            displayName = displayName,
            birthDate = patient.birthDate?.toString(),
            ageInfo = ageInfo,
            gender = patient.gender,
            primaryIdentifier = primaryIdentifier,
            contactInfo = contactInfo.take(2), // Limit to 2 most relevant contacts
            address = patient.addresses.find { it.use == "home" } ?: patient.addresses.firstOrNull()
        )
    }

    private fun createDocumentSummary(ipsDocument: IpsDocument): DocumentSummary {
        val totalItems = ipsDocument.medications.size + ipsDocument.allergies.size + 
                        ipsDocument.conditions.size + ipsDocument.immunizations.size +
                        ipsDocument.procedures.size + ipsDocument.observations.size +
                        ipsDocument.diagnosticReports.size

        val lastUpdated = ipsDocument.composition?.date

        return DocumentSummary(
            totalClinicalItems = totalItems,
            lastUpdated = lastUpdated,
            documentTitle = ipsDocument.composition?.title ?: "International Patient Summary",
            author = ipsDocument.composition?.author
        )
    }

    private fun createSections(ipsDocument: IpsDocument): List<IpsSection> {
        val sections = mutableListOf<IpsSection>()

        // Active medications
        if (ipsDocument.medications.isNotEmpty()) {
            val activeMeds = ipsDocument.medications.filter { 
                it.status == null || it.status.equals("active", ignoreCase = true) 
            }
            sections.add(IpsSection(
                title = "Current Medications",
                code = "medications",
                text = if (activeMeds.isNotEmpty()) 
                    "${activeMeds.size} active medications" 
                else "No active medications",
                resourceCount = activeMeds.size
            ))
        }

        // Active allergies
        if (ipsDocument.allergies.isNotEmpty()) {
            val activeAllergies = ipsDocument.allergies.filter {
                it.clinicalStatus == null || it.clinicalStatus.equals("active", ignoreCase = true)
            }
            sections.add(IpsSection(
                title = "Allergies & Intolerances",
                code = "allergies",
                text = if (activeAllergies.isNotEmpty()) 
                    "${activeAllergies.size} known allergies/intolerances" 
                else "No known allergies",
                resourceCount = activeAllergies.size
            ))
        }

        // Active conditions
        if (ipsDocument.conditions.isNotEmpty()) {
            val activeConditions = ipsDocument.conditions.filter {
                it.clinicalStatus == null || it.clinicalStatus.equals("active", ignoreCase = true)
            }
            sections.add(IpsSection(
                title = "Medical Conditions",
                code = "conditions",
                text = "${activeConditions.size} active conditions",
                resourceCount = activeConditions.size
            ))
        }

        // Immunizations
        if (ipsDocument.immunizations.isNotEmpty()) {
            sections.add(IpsSection(
                title = "Immunizations",
                code = "immunizations",
                text = "${ipsDocument.immunizations.size} vaccination records",
                resourceCount = ipsDocument.immunizations.size
            ))
        }

        // Recent procedures
        if (ipsDocument.procedures.isNotEmpty()) {
            sections.add(IpsSection(
                title = "Procedures",
                code = "procedures",
                text = "${ipsDocument.procedures.size} recorded procedures",
                resourceCount = ipsDocument.procedures.size
            ))
        }

        // Lab results and observations
        if (ipsDocument.observations.isNotEmpty() || ipsDocument.diagnosticReports.isNotEmpty()) {
            val totalLabItems = ipsDocument.observations.size + ipsDocument.diagnosticReports.size
            sections.add(IpsSection(
                title = "Laboratory Results",
                code = "laboratory",
                text = "$totalLabItems lab results and observations",
                resourceCount = totalLabItems
            ))
        }

        return sections
    }

    private fun generateAlerts(ipsDocument: IpsDocument): List<ClinicalAlert> {
        val alerts = mutableListOf<ClinicalAlert>()

        // Critical allergies
        val criticalAllergies = ipsDocument.allergies.filter { 
            it.criticality?.equals("high", ignoreCase = true) == true 
        }
        if (criticalAllergies.isNotEmpty()) {
            alerts.add(ClinicalAlert(
                level = AlertLevel.HIGH,
                title = "Critical Allergies",
                message = "${criticalAllergies.size} high-criticality allergies noted",
                details = criticalAllergies.map { it.substance ?: "Unknown substance" }
            ))
        }

        // Active high-severity conditions
        val severeConditions = ipsDocument.conditions.filter {
            it.severity?.contains("severe", ignoreCase = true) == true &&
            (it.clinicalStatus == null || it.clinicalStatus.equals("active", ignoreCase = true))
        }
        if (severeConditions.isNotEmpty()) {
            alerts.add(ClinicalAlert(
                level = AlertLevel.MEDIUM,
                title = "Severe Conditions",
                message = "${severeConditions.size} severe medical conditions",
                details = severeConditions.map { it.name ?: "Unknown condition" }
            ))
        }

        // Incomplete vaccination series
        val incompleteVaccinations = ipsDocument.immunizations.filter { immunization ->
            immunization.doseNumber != null && immunization.seriesDoses != null &&
            immunization.doseNumber!! < immunization.seriesDoses!!
        }.groupBy { it.vaccineName }

        if (incompleteVaccinations.isNotEmpty()) {
            alerts.add(ClinicalAlert(
                level = AlertLevel.LOW,
                title = "Incomplete Vaccinations",
                message = "${incompleteVaccinations.size} vaccine series may be incomplete",
                details = incompleteVaccinations.keys.filterNotNull()
            ))
        }

        return alerts
    }

    private fun extractMetadata(ipsDocument: IpsDocument): IpsMetadata {
        val resourceCounts = mutableMapOf<String, Int>()
        
        if (ipsDocument.medications.isNotEmpty()) resourceCounts["Medications"] = ipsDocument.medications.size
        if (ipsDocument.allergies.isNotEmpty()) resourceCounts["Allergies"] = ipsDocument.allergies.size
        if (ipsDocument.conditions.isNotEmpty()) resourceCounts["Conditions"] = ipsDocument.conditions.size
        if (ipsDocument.immunizations.isNotEmpty()) resourceCounts["Immunizations"] = ipsDocument.immunizations.size
        if (ipsDocument.procedures.isNotEmpty()) resourceCounts["Procedures"] = ipsDocument.procedures.size
        if (ipsDocument.observations.isNotEmpty()) resourceCounts["Observations"] = ipsDocument.observations.size
        if (ipsDocument.diagnosticReports.isNotEmpty()) resourceCounts["Diagnostic Reports"] = ipsDocument.diagnosticReports.size
        
        resourceCounts.putAll(ipsDocument.otherResources)

        return IpsMetadata(
            documentType = "International Patient Summary",
            fhirVersion = "R4",
            resourceCounts = resourceCounts,
            totalResources = resourceCounts.values.sum() + 1 // +1 for patient
        )
    }
}

/**
 * Processed IPS document with organized data and business rules applied
 */
data class ProcessedIpsDocument(
    val patient: ProcessedPatientInfo,
    val summary: DocumentSummary,
    val sections: List<IpsSection>,
    val alerts: List<ClinicalAlert>,
    val metadata: IpsMetadata
)

/**
 * Processed patient information optimized for display
 */
data class ProcessedPatientInfo(
    val displayName: String,
    val birthDate: String?,
    val ageInfo: String?,
    val gender: String?,
    val primaryIdentifier: IpsIdentifier?,
    val contactInfo: List<IpsContactPoint>,
    val address: IpsAddress?
)

/**
 * Document summary information
 */
data class DocumentSummary(
    val totalClinicalItems: Int,
    val lastUpdated: String?,
    val documentTitle: String,
    val author: String?
)

/**
 * Clinical alerts for important patient information
 */
data class ClinicalAlert(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val details: List<String> = emptyList()
)

/**
 * Alert severity levels
 */
enum class AlertLevel {
    HIGH,    // Critical information (severe allergies, etc.)
    MEDIUM,  // Important information (severe conditions, etc.)
    LOW      // Informational (incomplete vaccinations, etc.)
}

/**
 * Document metadata
 */
data class IpsMetadata(
    val documentType: String,
    val fhirVersion: String,
    val resourceCounts: Map<String, Int>,
    val totalResources: Int
)