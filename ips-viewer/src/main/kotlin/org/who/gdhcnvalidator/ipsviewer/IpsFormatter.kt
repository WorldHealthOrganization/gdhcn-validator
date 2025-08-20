package org.who.gdhcnvalidator.ipsviewer

/**
 * Formatter for creating human-readable text representations of IPS data
 */
class IpsFormatter {

    /**
     * Creates a comprehensive text summary of the IPS document
     */
    fun formatIpsDocument(processedIps: ProcessedIpsDocument): String {
        val builder = StringBuilder()
        
        // Document header
        builder.append("${processedIps.summary.documentTitle}\n")
        builder.append("=" * processedIps.summary.documentTitle.length).append("\n\n")
        
        // Patient information
        builder.append(formatPatientInfo(processedIps.patient))
        builder.append("\n")
        
        // Clinical alerts
        if (processedIps.alerts.isNotEmpty()) {
            builder.append(formatAlerts(processedIps.alerts))
            builder.append("\n")
        }
        
        // Document summary
        builder.append(formatDocumentSummary(processedIps.summary))
        builder.append("\n")
        
        // Content sections
        if (processedIps.sections.isNotEmpty()) {
            builder.append("Document Contents:\n")
            processedIps.sections.forEach { section ->
                builder.append("• ${section.title}: ${section.text}\n")
            }
            builder.append("\n")
        }
        
        // Document metadata
        builder.append(formatMetadata(processedIps.metadata))
        
        return builder.toString()
    }

    /**
     * Creates a brief summary of the IPS document
     */
    fun formatBriefSummary(processedIps: ProcessedIpsDocument): String {
        val builder = StringBuilder()
        
        builder.append("Patient: ${processedIps.patient.displayName}\n")
        
        if (processedIps.patient.ageInfo != null) {
            builder.append("${processedIps.patient.ageInfo}")
            if (processedIps.patient.gender != null) {
                builder.append(", ${processedIps.patient.gender}")
            }
            builder.append("\n")
        } else if (processedIps.patient.gender != null) {
            builder.append("Gender: ${processedIps.patient.gender}\n")
        }
        
        if (processedIps.summary.totalClinicalItems > 0) {
            builder.append("Clinical Items: ${processedIps.summary.totalClinicalItems}\n")
        }
        
        if (processedIps.alerts.isNotEmpty()) {
            val highAlerts = processedIps.alerts.count { it.level == AlertLevel.HIGH }
            if (highAlerts > 0) {
                builder.append("⚠️ $highAlerts critical alerts\n")
            }
        }
        
        return builder.toString()
    }

    private fun formatPatientInfo(patient: ProcessedPatientInfo): String {
        val builder = StringBuilder()
        builder.append("Patient Information:\n")
        
        builder.append("• Name: ${patient.displayName}\n")
        
        if (patient.birthDate != null) {
            builder.append("• Birth Date: ${patient.birthDate}")
            if (patient.ageInfo != null) {
                builder.append(" (${patient.ageInfo})")
            }
            builder.append("\n")
        }
        
        if (patient.gender != null) {
            builder.append("• Gender: ${patient.gender}\n")
        }
        
        if (patient.primaryIdentifier != null) {
            val id = patient.primaryIdentifier
            val idType = id.type ?: "ID"
            builder.append("• $idType: ${id.value}\n")
        }
        
        if (patient.contactInfo.isNotEmpty()) {
            patient.contactInfo.forEach { contact ->
                val system = contact.system?.replaceFirstChar { it.uppercaseChar() } ?: "Contact"
                builder.append("• $system: ${contact.value}\n")
            }
        }
        
        if (patient.address != null) {
            val addr = patient.address
            val addressText = addr.text ?: buildAddressText(addr)
            if (addressText.isNotEmpty()) {
                builder.append("• Address: $addressText\n")
            }
        }
        
        return builder.toString()
    }

    private fun formatAlerts(alerts: List<ClinicalAlert>): String {
        val builder = StringBuilder()
        builder.append("Clinical Alerts:\n")
        
        // Sort by severity: HIGH -> MEDIUM -> LOW
        val sortedAlerts = alerts.sortedBy { 
            when (it.level) {
                AlertLevel.HIGH -> 0
                AlertLevel.MEDIUM -> 1
                AlertLevel.LOW -> 2
            }
        }
        
        sortedAlerts.forEach { alert ->
            val icon = when (alert.level) {
                AlertLevel.HIGH -> "🚨"
                AlertLevel.MEDIUM -> "⚠️"
                AlertLevel.LOW -> "ℹ️"
            }
            
            builder.append("$icon ${alert.title}: ${alert.message}\n")
            
            if (alert.details.isNotEmpty()) {
                alert.details.take(3).forEach { detail -> // Limit to 3 details
                    builder.append("  - $detail\n")
                }
                if (alert.details.size > 3) {
                    builder.append("  - ... and ${alert.details.size - 3} more\n")
                }
            }
        }
        
        return builder.toString()
    }

    private fun formatDocumentSummary(summary: DocumentSummary): String {
        val builder = StringBuilder()
        builder.append("Summary:\n")
        
        if (summary.totalClinicalItems > 0) {
            builder.append("• Total Clinical Items: ${summary.totalClinicalItems}\n")
        }
        
        if (summary.lastUpdated != null) {
            builder.append("• Last Updated: ${summary.lastUpdated}\n")
        }
        
        if (summary.author != null) {
            builder.append("• Author: ${summary.author}\n")
        }
        
        return builder.toString()
    }

    private fun formatMetadata(metadata: IpsMetadata): String {
        val builder = StringBuilder()
        builder.append("Document Details:\n")
        builder.append("• Type: ${metadata.documentType}\n")
        builder.append("• FHIR Version: ${metadata.fhirVersion}\n")
        builder.append("• Total Resources: ${metadata.totalResources}\n")
        
        if (metadata.resourceCounts.isNotEmpty()) {
            builder.append("• Resource Breakdown:\n")
            metadata.resourceCounts.forEach { (type, count) ->
                builder.append("  - $type: $count\n")
            }
        }
        
        return builder.toString()
    }

    private fun buildAddressText(address: IpsAddress): String {
        val parts = mutableListOf<String>()
        
        if (address.line.isNotEmpty()) {
            parts.addAll(address.line)
        }
        
        listOfNotNull(address.city, address.state, address.postalCode, address.country).let {
            if (it.isNotEmpty()) {
                parts.add(it.joinToString(", "))
            }
        }
        
        return parts.joinToString(", ")
    }

    /**
     * Helper extension for string repetition
     */
    private operator fun String.times(times: Int): String {
        return this.repeat(times)
    }
}