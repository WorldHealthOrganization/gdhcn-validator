package org.who.gdhcnvalidator.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.*
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.who.gdhcnvalidator.FhirApplication
import org.who.gdhcnvalidator.QRDecoder
import org.who.gdhcnvalidator.R
import org.who.gdhcnvalidator.databinding.FragmentResultBinding
import org.who.gdhcnvalidator.ipsviewer.IpsViewer
import org.who.gdhcnvalidator.ipsviewer.AlertLevel
import org.who.gdhcnvalidator.services.DDCCFormatter
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.verify.hcert.healthlink.VhlVerifier
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


/**
 * Displays a Verifiable Credential after being Scanned by the QRScan Fragment
 */
class ResultFragment : Fragment() {
    private var binding: FragmentResultBinding? = null
    private val args: ResultFragmentArgs by navArgs()

    private val statuses = mapOf(
        QRDecoder.Status.NOT_FOUND to R.string.verification_status_not_found,
        QRDecoder.Status.NOT_SUPPORTED to R.string.verification_status_invalid_base45,
        QRDecoder.Status.INVALID_ENCODING to R.string.verification_status_invalid_base45,
        QRDecoder.Status.INVALID_COMPRESSION to R.string.verification_status_invalid_zip,
        QRDecoder.Status.INVALID_SIGNING_FORMAT to R.string.verification_status_invalid_cose,
        QRDecoder.Status.KID_NOT_INCLUDED to R.string.verification_status_kid_not_included,
        QRDecoder.Status.ISSUER_NOT_TRUSTED to R.string.verification_status_issuer_not_trusted,
        QRDecoder.Status.TERMINATED_KEYS to R.string.verification_status_terminated_keys,
        QRDecoder.Status.EXPIRED_KEYS to R.string.verification_status_expired_keys,
        QRDecoder.Status.REVOKED_KEYS to R.string.verification_status_revoked_keys,
        QRDecoder.Status.INVALID_SIGNATURE to R.string.verification_status_invalid_signature,
        QRDecoder.Status.VERIFIED to R.string.verification_status_verified,
        // VHL specific statuses (we'll need to add these strings)
        QRDecoder.Status.VHL_REQUIRES_PIN to R.string.vhl_status_requires_pin,
        QRDecoder.Status.VHL_INVALID_URI to R.string.vhl_status_invalid_uri,
        QRDecoder.Status.VHL_FETCH_ERROR to R.string.vhl_status_fetch_error,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    private fun setTextView(view: TextView?, text: String?, line: View?) {
        if (text != null && text.isNotEmpty()) {
            view?.text = text
            line?.visibility = TextView.VISIBLE
        } else
            line?.visibility = TextView.GONE
    }

    data class ResultCard(
        val hcid: String?,
        val cardTitle: String?,
        val validUntil: String?,

        val personName: String?,
        val personDetails: String?,
        val identifier: String?,

        val location: String?,
        val pha: String?,
        val hw: String?,

        // testresults
        val testType: String?,
        val testTypeDetail: String?,
        val testDate: String?,
        val testResult: String?,

        // immunization
        val dose: String?,
        val doseDate: String?,
        val vaccineValid: String?,
        val vaccineAgainst: String?,
        val vaccineType: String?,
        val vaccineInfo: String?,
        val vaccineInfo2: String?,

        // recommendations
        val nextDose: String?,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.tvResultHeader?.visibility = TextView.INVISIBLE
        binding?.tvResultCard?.visibility = TextView.INVISIBLE

        if (args.qr != null) {
            resolveAndShowQR(args.qr!!)
        }

        binding?.btResultClose?.setOnClickListener {
            findNavController().navigate(R.id.action_ResultFragment_to_HomeFragment)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateScreen(DDCC: QRDecoder.VerificationResult) {
        binding?.tvResultHeader?.visibility = TextView.VISIBLE

        statuses[DDCC.status]?.let {
            binding?.tvResultTitle?.text = resources.getString(it)
        }

        if (DDCC.status == QRDecoder.Status.VERIFIED) {
            if (DDCC.issuer!!.scope == TrustRegistry.Scope.PRODUCTION) {
                binding?.tvResultHeader?.background = resources.getDrawable(R.drawable.rounded_pill, null)
                binding?.tvResultTitle?.text = resources.getString(R.string.verification_status_verified)
            } else {
                binding?.tvResultHeader?.background = resources.getDrawable(R.drawable.rounded_pill_test, null)
                binding?.tvResultTitle?.text = resources.getString(R.string.verification_status_verified_test_scope)
            }
            binding?.tvResultTitleIcon?.setImageResource(R.drawable.check_circle_24px)
        } else {
            binding?.tvResultHeader?.background = resources.getDrawable(R.drawable.rounded_pill_invalid, null)
            binding?.tvResultTitleIcon?.setImageResource(R.drawable.cancel_24px)
        }

        if (DDCC.issuer != null) {
            binding?.tvResultSignedBy?.text = "Signed by ${DDCC.issuer!!.displayName["en"]}"
            if (DDCC.issuer!!.scope == TrustRegistry.Scope.PRODUCTION) {
                binding?.tvResultSignedByIconFailed?.visibility = View.GONE
                binding?.tvResultSignedByIconUntrusted?.visibility = View.GONE
                binding?.tvResultSignedByIconSuccess?.visibility = View.VISIBLE
            } else {
                binding?.tvResultSignedByIconFailed?.visibility = View.GONE
                binding?.tvResultSignedByIconUntrusted?.visibility = View.VISIBLE
                binding?.tvResultSignedByIconSuccess?.visibility = View.GONE
            }
        } else {
            binding?.tvResultSignedBy?.text = resources.getString(R.string.verification_status_invalid_signature)
            binding?.tvResultSignedByIconFailed?.visibility = View.VISIBLE
            binding?.tvResultSignedByIconUntrusted?.visibility = View.GONE
            binding?.tvResultSignedByIconSuccess?.visibility = View.GONE
        }

        if (DDCC.contents != null) {
            binding?.tvResultCard?.visibility = TextView.VISIBLE

            // Check if this is a VHL result with file list
            if (DDCC.vhlInfo?.fileList != null) {
                showVhlFileList(DDCC.vhlInfo.fileList)
            } else {
                // Traditional health certificate display
                val card = DDCCFormatter().run(DDCC.composition()!!)

                // Credential
                setTextView(binding?.tvResultScanDate, card.cardTitle, binding?.tvResultScanDate)
                setTextView(binding?.tvResultValidUntil, card.validUntil, binding?.llResultValidUntil)

                // Patient
                setTextView(binding?.tvResultName, card.personName, binding?.tvResultName)
                setTextView(binding?.tvResultPersonDetails, card.personDetails, binding?.tvResultPersonDetails)
                setTextView(binding?.tvResultIdentifier, card.identifier, binding?.tvResultIdentifier)

                // Location, Practice, Practitioner
                setTextView(binding?.tvResultHcid, card.hcid, binding?.llResultHcid)
                setTextView(binding?.tvResultPha, card.pha, binding?.llResultPha)
                setTextView(binding?.tvResultHw, card.hw, binding?.llResultHw)

                // Test Result
                setTextView(binding?.tvResultTestType, card.testType, binding?.tvResultTestType)
                setTextView(binding?.tvResultTestTypeDetail, card.testTypeDetail, binding?.llResultTestTypeDetail)
                setTextView(binding?.tvResultTestDate, card.testDate, binding?.llResultTestDate)
                setTextView(binding?.tvResultTestTitle, card.testResult, binding?.tvResultTestTitle)

                // Immunization
                setTextView(binding?.tvResultVaccineType, card.vaccineType, binding?.tvResultVaccineType)
                setTextView(binding?.tvResultDoseTitle, card.dose, binding?.tvResultDoseTitle)
                setTextView(binding?.tvResultDoseDate, card.doseDate, binding?.llResultDoseDate)
                setTextView(binding?.tvResultVaccineValid, card.vaccineValid, binding?.llResultVaccineValid)
                setTextView(binding?.tvResultVaccineInfo, card.vaccineInfo, binding?.llResultVaccineInfo)
                setTextView(binding?.tvResultVaccineInfo2, card.vaccineInfo2, binding?.llResultVaccineInfo2)
                setTextView(binding?.tvResultCentre, card.location, binding?.llResultCentre)

                // Recommendation
                setTextView(binding?.tvResultNextDose, card.nextDose, binding?.llResultNextDose)

                // Status
                binding?.llResultStatus?.removeAllViews()
            }
        } else if (DDCC.status == QRDecoder.Status.VHL_REQUIRES_PIN) {
            // Show PIN entry interface
            showVhlPinEntry(DDCC)
        }
    }

    private fun resolveAndShowQR(qr: String) = runBlocking {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            withContext(Dispatchers.IO) {
                val result = resolveQR(qr)

                withContext(Dispatchers.Main){
                    updateScreen(result)
                }

                val bundle = result.contents

                if (bundle != null) {
                    saveContents(bundle)
                    computeAndShowStatus(patId(bundle))
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun computeAndShowStatus(patientId: String) = runBlocking {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            withContext(Dispatchers.IO) {
                val results = context?.let {
                    FhirApplication.subscribedIGs(it).associate {
                        val (status, elapsed) = measureTimedValue {
                            resolveStatus(patientId, it.url, "Completed Immunization")
                        }
                        println("TIME: Evaluation of ${it.url} in $elapsed")
                        Pair(it.name, status)
                    }
                }

                if (results != null) {
                    withContext(Dispatchers.Main) {
                        binding?.let {
                            results.forEach {
                                addResultStatus(it.key, it.value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addResultStatus(ruleName: String, result: Boolean?) {
        binding?.llResultStatus?.addView(TextView(context).apply {
            text = when (result) {
                true -> "${ruleName}: COVID Safe"
                false -> "${ruleName}: COVID Vulnerable"
                null -> "${ruleName}: Unable to evaluate"
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
    }

    private fun patId(bundle: org.hl7.fhir.r4.model.Bundle): String {
        return bundle.entry.filter { it.resource is Patient }.first().resource.id.removePrefix("Patient/")
    }

    private suspend fun saveContents(bundle: org.hl7.fhir.r4.model.Bundle) {
        for (entry in bundle.entry) {
            FhirApplication.fhirEngine(requireContext()).create(entry.resource)
        }
    }

    private fun resolveQR(qr: String): QRDecoder.VerificationResult {
        return QRDecoder(FhirApplication.trustRegistry(requireContext())).decode(qr)
    }

    private fun resolveStatus(patientId: String, libUrl: String, funcName: String): Boolean? {
        // Might be slow
        return try {
            val results = FhirApplication.fhirOperator(requireContext()).evaluateLibrary(
                libUrl,
                patientId,
                setOf(funcName)) as Parameters

            results.getParameterBool(funcName)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
    
    /**
     * Shows PIN entry dialog for VHL
     */
    private fun showVhlPinEntry(vhlResult: QRDecoder.VerificationResult) {
        val input = EditText(requireContext())
        input.hint = "Enter PIN"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        AlertDialog.Builder(requireContext())
            .setTitle("PIN Required")
            .setMessage("This Verifiable Health Link requires a PIN to access the manifest.")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val pin = input.text.toString()
                fetchVhlManifestWithPin(vhlResult, pin)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
    
    /**
     * Fetches VHL manifest with PIN and updates the display
     */
    private fun fetchVhlManifestWithPin(vhlResult: QRDecoder.VerificationResult, pin: String) {
        CoroutineScope(Dispatchers.Main + Job()).launch {
            val decodedLink = vhlResult.vhlInfo?.decodedLink
            if (decodedLink != null) {
                withContext(Dispatchers.IO) {
                    val vhlVerifier = VhlVerifier()
                    val request = VhlVerifier.VhlManifestRequest(decodedLink.url, pin)
                    val manifest = vhlVerifier.fetchManifest(request)
                    
                    withContext(Dispatchers.Main) {
                        if (manifest != null) {
                            val fileList = vhlVerifier.extractFileList(manifest)
                            showVhlFileList(fileList)
                        } else {
                            Toast.makeText(requireContext(), "Failed to fetch manifest. Check your PIN.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Shows the list of files available in the VHL manifest
     */
    private fun showVhlFileList(fileList: List<VhlVerifier.VhlFileInfo>) {
        // Clear existing content and show file list
        clearResultFields()
        
        // Get the main content container
        val resultCard = binding?.root?.findViewById<LinearLayout>(R.id.tv_result_card2)
        resultCard?.removeAllViews()
        
        // Add header
        val header = TextView(requireContext())
        header.text = "Available Files"
        header.textSize = 18f
        header.setTypeface(null, android.graphics.Typeface.BOLD)
        header.setPadding(0, 0, 0, 16)
        resultCard?.addView(header)
        
        // Add each file as a clickable item
        fileList.forEach { file ->
            val fileItem = createFileListItem(file)
            resultCard?.addView(fileItem)
        }
        
        binding?.tvResultCard?.visibility = View.VISIBLE
    }
    
    /**
     * Creates a clickable item for each file in the VHL manifest
     */
    private fun createFileListItem(file: VhlVerifier.VhlFileInfo): View {
        val fileItem = LinearLayout(requireContext())
        fileItem.orientation = LinearLayout.HORIZONTAL
        fileItem.setPadding(16, 16, 16, 16)
        fileItem.isClickable = true
        
        // Add some background styling
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        fileItem.setBackgroundResource(typedValue.resourceId)
        
        // File icon based on type
        val icon = ImageView(requireContext())
        when (file.type) {
            "PDF" -> icon.setImageResource(android.R.drawable.ic_menu_edit)
            "FHIR_IPS" -> icon.setImageResource(android.R.drawable.ic_menu_info_details)
            else -> icon.setImageResource(android.R.drawable.ic_menu_help)
        }
        icon.layoutParams = LinearLayout.LayoutParams(64, 64)
        fileItem.addView(icon)
        
        // File details
        val textContainer = LinearLayout(requireContext())
        textContainer.orientation = LinearLayout.VERTICAL
        textContainer.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        textContainer.setPadding(16, 0, 0, 0)
        
        val title = TextView(requireContext())
        title.text = file.title
        title.textSize = 16f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        textContainer.addView(title)
        
        val subtitle = TextView(requireContext())
        subtitle.text = "Type: ${file.type}"
        subtitle.textSize = 14f
        textContainer.addView(subtitle)
        
        if (file.size != null) {
            val size = TextView(requireContext())
            size.text = "Size: ${file.size} bytes"
            size.textSize = 12f
            textContainer.addView(size)
        }
        
        fileItem.addView(textContainer)
        
        // Click handler
        fileItem.setOnClickListener {
            handleFileClick(file)
        }
        
        // Add some margin between items
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 8, 0, 8)
        fileItem.layoutParams = layoutParams
        
        return fileItem
    }
    
    /**
     * Handles clicks on file items
     */
    private fun handleFileClick(file: VhlVerifier.VhlFileInfo) {
        when (file.type) {
            "PDF" -> {
                if (file.url != null) {
                    // Open PDF in browser or external app
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(file.url))
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "PDF URL not available", Toast.LENGTH_SHORT).show()
                }
            }
            "FHIR_IPS" -> {
                // Display FHIR IPS content
                showFhirIpsDialog(file)
            }
            else -> {
                Toast.makeText(requireContext(), "File type not supported: ${file.type}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Shows FHIR IPS content in a dialog with structured information using the IPS library
     */
    private fun showFhirIpsDialog(file: VhlVerifier.VhlFileInfo) {
        val ipsBundle = file.content as? org.hl7.fhir.r4.model.Bundle
        
        if (ipsBundle == null) {
            AlertDialog.Builder(requireContext())
                .setTitle(file.title)
                .setMessage("FHIR IPS Document\n\nContent is not available for display.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        try {
            val ipsViewer = IpsViewer()
            
            // Check if it's a valid IPS bundle
            if (!ipsViewer.isValidIpsBundle(ipsBundle)) {
                showBasicBundleInfo(file, ipsBundle)
                return
            }
            
            // Process the IPS document
            val processedIps = ipsViewer.processIpsBundle(ipsBundle)
            
            // Create a custom dialog with structured content
            showStructuredIpsDialog(file, processedIps)
            
        } catch (e: Exception) {
            // Fallback to basic display if IPS processing fails
            showBasicBundleInfo(file, ipsBundle)
        }
    }

    /**
     * Shows a structured IPS dialog with rich content
     */
    private fun showStructuredIpsDialog(file: VhlVerifier.VhlFileInfo, processedIps: org.who.gdhcnvalidator.ipsviewer.ProcessedIpsDocument) {
        val context = requireContext()
        
        // Create a scrollable view for the content
        val scrollView = ScrollView(context)
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(24, 16, 24, 16)
        
        // Patient header
        addPatientHeader(container, processedIps.patient)
        
        // Clinical alerts
        if (processedIps.alerts.isNotEmpty()) {
            addClinicalAlerts(container, processedIps.alerts)
        }
        
        // Document sections
        addDocumentSections(container, processedIps.sections)
        
        // Document summary
        addDocumentSummary(container, processedIps.summary, processedIps.metadata)
        
        scrollView.addView(container)
        
        AlertDialog.Builder(context)
            .setTitle(file.title)
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Details") { _, _ ->
                // Show detailed text view
                showDetailedIpsText(file, processedIps)
            }
            .show()
    }

    /**
     * Shows detailed text representation of the IPS
     */
    private fun showDetailedIpsText(file: VhlVerifier.VhlFileInfo, processedIps: org.who.gdhcnvalidator.ipsviewer.ProcessedIpsDocument) {
        val ipsViewer = IpsViewer()
        val ipsBundle = file.content as org.hl7.fhir.r4.model.Bundle
        val detailedText = ipsViewer.formatIpsAsText(ipsBundle)
        
        val scrollView = ScrollView(requireContext())
        val textView = TextView(requireContext())
        textView.text = detailedText
        textView.setTextIsSelectable(true)
        textView.typeface = android.graphics.Typeface.MONOSPACE
        textView.textSize = 12f
        textView.setPadding(16, 16, 16, 16)
        scrollView.addView(textView)
        
        AlertDialog.Builder(requireContext())
            .setTitle("${file.title} - Details")
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun addPatientHeader(container: LinearLayout, patient: org.who.gdhcnvalidator.ipsviewer.ProcessedPatientInfo) {
        val context = container.context
        
        // Patient name
        val nameView = TextView(context)
        nameView.text = patient.displayName
        nameView.textSize = 18f
        nameView.setTypeface(null, android.graphics.Typeface.BOLD)
        nameView.setTextColor(context.getColor(android.R.color.black))
        container.addView(nameView)
        
        // Patient details
        val detailsLayout = LinearLayout(context)
        detailsLayout.orientation = LinearLayout.HORIZONTAL
        
        val details = mutableListOf<String>()
        if (patient.ageInfo != null) details.add(patient.ageInfo)
        if (patient.gender != null) details.add(patient.gender)
        
        if (details.isNotEmpty()) {
            val detailsView = TextView(context)
            detailsView.text = details.joinToString(" • ")
            detailsView.textSize = 14f
            detailsView.setTextColor(context.getColor(android.R.color.darker_gray))
            container.addView(detailsView)
        }
        
        // Identifier
        if (patient.primaryIdentifier != null) {
            val idView = TextView(context)
            val idType = patient.primaryIdentifier.type ?: "ID"
            idView.text = "$idType: ${patient.primaryIdentifier.value}"
            idView.textSize = 12f
            idView.setTextColor(context.getColor(android.R.color.darker_gray))
            container.addView(idView)
        }
        
        // Add separator
        addSeparator(container)
    }

    private fun addClinicalAlerts(container: LinearLayout, alerts: List<org.who.gdhcnvalidator.ipsviewer.ClinicalAlert>) {
        val context = container.context
        
        val alertHeader = TextView(context)
        alertHeader.text = "Clinical Alerts"
        alertHeader.textSize = 16f
        alertHeader.setTypeface(null, android.graphics.Typeface.BOLD)
        container.addView(alertHeader)
        
        alerts.forEach { alert ->
            val alertLayout = LinearLayout(context)
            alertLayout.orientation = LinearLayout.HORIZONTAL
            alertLayout.setPadding(0, 8, 0, 8)
            
            // Alert icon
            val iconView = TextView(context)
            iconView.text = when (alert.level) {
                AlertLevel.HIGH -> "🚨"
                AlertLevel.MEDIUM -> "⚠️"
                AlertLevel.LOW -> "ℹ️"
            }
            iconView.textSize = 16f
            iconView.setPadding(0, 0, 8, 0)
            alertLayout.addView(iconView)
            
            // Alert content
            val alertContent = LinearLayout(context)
            alertContent.orientation = LinearLayout.VERTICAL
            alertContent.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            
            val titleView = TextView(context)
            titleView.text = alert.title
            titleView.textSize = 14f
            titleView.setTypeface(null, android.graphics.Typeface.BOLD)
            alertContent.addView(titleView)
            
            val messageView = TextView(context)
            messageView.text = alert.message
            messageView.textSize = 12f
            alertContent.addView(messageView)
            
            alertLayout.addView(alertContent)
            container.addView(alertLayout)
        }
        
        addSeparator(container)
    }

    private fun addDocumentSections(container: LinearLayout, sections: List<org.who.gdhcnvalidator.ipsviewer.IpsSection>) {
        val context = container.context
        
        if (sections.isNotEmpty()) {
            val sectionHeader = TextView(context)
            sectionHeader.text = "Document Contents"
            sectionHeader.textSize = 16f
            sectionHeader.setTypeface(null, android.graphics.Typeface.BOLD)
            container.addView(sectionHeader)
            
            sections.forEach { section ->
                val sectionLayout = LinearLayout(context)
                sectionLayout.orientation = LinearLayout.HORIZONTAL
                sectionLayout.setPadding(0, 4, 0, 4)
                
                val bullet = TextView(context)
                bullet.text = "•"
                bullet.textSize = 14f
                bullet.setPadding(0, 0, 8, 0)
                sectionLayout.addView(bullet)
                
                val sectionText = TextView(context)
                sectionText.text = "${section.title}: ${section.text}"
                sectionText.textSize = 14f
                sectionText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                sectionLayout.addView(sectionText)
                
                container.addView(sectionLayout)
            }
            
            addSeparator(container)
        }
    }

    private fun addDocumentSummary(
        container: LinearLayout, 
        summary: org.who.gdhcnvalidator.ipsviewer.DocumentSummary,
        metadata: org.who.gdhcnvalidator.ipsviewer.IpsMetadata
    ) {
        val context = container.context
        
        val summaryHeader = TextView(context)
        summaryHeader.text = "Document Information"
        summaryHeader.textSize = 16f
        summaryHeader.setTypeface(null, android.graphics.Typeface.BOLD)
        container.addView(summaryHeader)
        
        if (summary.totalClinicalItems > 0) {
            addInfoItem(container, "Clinical Items", summary.totalClinicalItems.toString())
        }
        
        if (summary.lastUpdated != null) {
            addInfoItem(container, "Last Updated", summary.lastUpdated)
        }
        
        if (summary.author != null) {
            addInfoItem(container, "Author", summary.author)
        }
        
        addInfoItem(container, "Document Type", metadata.documentType)
        addInfoItem(container, "FHIR Version", metadata.fhirVersion)
        addInfoItem(container, "Total Resources", metadata.totalResources.toString())
    }

    private fun addInfoItem(container: LinearLayout, label: String, value: String) {
        val context = container.context
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(0, 2, 0, 2)
        
        val labelView = TextView(context)
        labelView.text = "$label:"
        labelView.textSize = 12f
        labelView.setTypeface(null, android.graphics.Typeface.BOLD)
        labelView.setPadding(0, 0, 8, 0)
        layout.addView(labelView)
        
        val valueView = TextView(context)
        valueView.text = value
        valueView.textSize = 12f
        valueView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        layout.addView(valueView)
        
        container.addView(layout)
    }

    private fun addSeparator(container: LinearLayout) {
        val context = container.context
        val separator = View(context)
        separator.setBackgroundColor(context.getColor(android.R.color.darker_gray))
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        params.setMargins(0, 16, 0, 16)
        separator.layoutParams = params
        container.addView(separator)
    }

    /**
     * Fallback method for non-IPS bundles or when IPS processing fails
     */
    private fun showBasicBundleInfo(file: VhlVerifier.VhlFileInfo, bundle: org.hl7.fhir.r4.model.Bundle) {
        val message = buildBasicBundleText(bundle)
        
        AlertDialog.Builder(requireContext())
            .setTitle(file.title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    /**
     * Builds basic text representation for non-IPS FHIR bundles
     */
    private fun buildBasicBundleText(bundle: org.hl7.fhir.r4.model.Bundle): String {
        val builder = StringBuilder()
        builder.append("FHIR Bundle Document\n\n")
        
        // Extract patient information if available
        val patient = bundle.entry?.find { 
            it.resource is org.hl7.fhir.r4.model.Patient 
        }?.resource as? org.hl7.fhir.r4.model.Patient
        
        if (patient != null) {
            builder.append("Patient Information:\n")
            patient.name?.firstOrNull()?.let { name ->
                val fullName = listOfNotNull(
                    name.given?.joinToString(" "),
                    name.family
                ).joinToString(" ")
                if (fullName.isNotEmpty()) {
                    builder.append("• Name: $fullName\n")
                }
            }
            
            patient.birthDate?.let { birthDate ->
                builder.append("• Birth Date: ${birthDate}\n")
            }
            
            patient.gender?.let { gender ->
                builder.append("• Gender: ${gender.display ?: gender.name}\n")
            }
            
            builder.append("\n")
        }
        
        // Count resources
        val resourceCounts = bundle.entry?.groupBy { 
            it.resource?.fhirType() 
        }?.mapValues { it.value.size }
        
        if (resourceCounts != null && resourceCounts.isNotEmpty()) {
            builder.append("Bundle Contents:\n")
            resourceCounts.forEach { (type, count) ->
                builder.append("• $type: $count entries\n")
            }
        }
        
        builder.append("\nThis document contains structured health information in FHIR format.")
        
        return builder.toString()
    }
    
    /**
     * Clears all result fields for VHL display
     */
    private fun clearResultFields() {
        binding?.tvResultScanDate?.visibility = View.GONE
        binding?.llResultValidUntil?.visibility = View.GONE
        binding?.tvResultName?.visibility = View.GONE
        binding?.tvResultPersonDetails?.visibility = View.GONE
        binding?.tvResultIdentifier?.visibility = View.GONE
        binding?.llResultHcid?.visibility = View.GONE
        binding?.llResultPha?.visibility = View.GONE
        binding?.llResultHw?.visibility = View.GONE
        binding?.tvResultTestType?.visibility = View.GONE
        binding?.llResultTestTypeDetail?.visibility = View.GONE
        binding?.llResultTestDate?.visibility = View.GONE
        binding?.tvResultTestTitle?.visibility = View.GONE
        binding?.tvResultVaccineType?.visibility = View.GONE
        binding?.tvResultDoseTitle?.visibility = View.GONE
        binding?.llResultDoseDate?.visibility = View.GONE
        binding?.llResultVaccineValid?.visibility = View.GONE
        binding?.llResultVaccineInfo?.visibility = View.GONE
        binding?.llResultVaccineInfo2?.visibility = View.GONE
        binding?.llResultCentre?.visibility = View.GONE
        binding?.llResultNextDose?.visibility = View.GONE
        binding?.llResultStatus?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}