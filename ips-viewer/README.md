# IPS Viewer - Kotlin Library for FHIR International Patient Summary

A comprehensive Kotlin library for parsing, processing, and displaying FHIR International Patient Summary (IPS) documents. This library provides structured data extraction, clinical alert detection, and formatted display capabilities for IPS bundles.

## Features

### 🔍 **FHIR IPS Parsing**
- Extracts structured data from FHIR R4 Bundle resources
- Parses patient demographics, medications, allergies, conditions, immunizations, and more
- Handles multiple date formats and resource relationships
- Validates IPS bundle structure

### 🧠 **Intelligent Processing**
- Applies clinical business rules for data organization
- Generates clinical alerts for critical information (severe allergies, conditions)
- Prioritizes active vs inactive medical information
- Creates structured sections for easy consumption

### 📊 **Rich Data Models**
- Comprehensive data classes for all IPS components
- Type-safe representations of clinical concepts
- Structured patient information with demographics and identifiers
- Clinical alerts with severity levels (HIGH, MEDIUM, LOW)

### 📝 **Flexible Display**
- Multiple output formats: structured data, formatted text, brief summaries
- Customizable formatting for different display contexts
- Human-readable text generation with proper medical terminology
- Platform-agnostic output suitable for Android, web, or console display

## Architecture

The library follows a clean, modular architecture:

```
IpsViewer (Facade)
├── IpsParser      # Raw FHIR → Structured Data
├── IpsProcessor   # Business Logic & Organization  
└── IpsFormatter   # Display & Text Generation
```

### Core Components

- **`IpsParser`**: Converts FHIR Bundle resources into structured Kotlin data classes
- **`IpsProcessor`**: Applies clinical business rules and generates alerts
- **`IpsFormatter`**: Creates human-readable text representations
- **`IpsViewer`**: Main facade providing simple API access
- **`SafeIpsViewer`**: Error-safe version returning Result types

## Usage Examples

### Basic IPS Processing

```kotlin
import org.who.gdhcnvalidator.ipsviewer.IpsViewer
import org.hl7.fhir.r4.model.Bundle

val ipsViewer = IpsViewer()
val ipsBundle: Bundle = // ... your FHIR Bundle

// Validate IPS bundle
if (ipsViewer.isValidIpsBundle(ipsBundle)) {
    // Process the IPS document
    val processedIps = ipsViewer.processIpsBundle(ipsBundle)
    
    // Access structured data
    println("Patient: ${processedIps.patient.displayName}")
    println("Age: ${processedIps.patient.ageInfo}")
    
    // Check for clinical alerts
    processedIps.alerts.forEach { alert ->
        println("${alert.level}: ${alert.title} - ${alert.message}")
    }
}
```

### Text Formatting

```kotlin
// Generate comprehensive text summary
val fullText = ipsViewer.formatIpsAsText(ipsBundle)
println(fullText)

// Generate brief summary
val briefSummary = ipsViewer.formatIpsBrief(ipsBundle)
println(briefSummary)
```

### Safe Error Handling

```kotlin
import org.who.gdhcnvalidator.ipsviewer.SafeIpsViewer
import org.who.gdhcnvalidator.ipsviewer.IpsResult

val safeViewer = SafeIpsViewer()

when (val result = safeViewer.processIpsBundle(ipsBundle)) {
    is IpsResult.Success -> {
        val processedIps = result.data
        // Handle successful processing
    }
    is IpsResult.Error -> {
        println("Error: ${result.message}")
        // Handle error case
    }
}
```

### Clinical Information Access

```kotlin
// Extract specific clinical data
val patientInfo = ipsViewer.extractPatientInfo(ipsBundle)
val alerts = ipsViewer.getClinicalAlerts(ipsBundle)
val metadata = ipsViewer.getIpsMetadata(ipsBundle)

// Access detailed clinical data from processed IPS
val processedIps = ipsViewer.processIpsBundle(ipsBundle)

processedIps.sections.forEach { section ->
    println("${section.title}: ${section.text}")
}
```

## Data Models

### Key Data Classes

- **`ProcessedIpsDocument`**: Complete processed IPS with all components
- **`ProcessedPatientInfo`**: Patient demographics optimized for display
- **`ClinicalAlert`**: Important clinical information with severity levels
- **`IpsSection`**: Organized clinical content sections
- **`IpsMetadata`**: Document metadata and resource counts

### Supported FHIR Resources

- **Patient**: Demographics, identifiers, contact information
- **Composition**: Document structure and metadata
- **MedicationStatement/Medication**: Current and past medications
- **AllergyIntolerance**: Allergies and intolerances with reactions
- **Condition**: Medical conditions and problems
- **Immunization**: Vaccination records
- **Procedure**: Medical procedures
- **Observation**: Clinical observations and vital signs
- **DiagnosticReport**: Laboratory results and reports

## Clinical Alerts

The library automatically generates clinical alerts for:

- **HIGH Priority**: Critical allergies with high criticality
- **MEDIUM Priority**: Severe active medical conditions
- **LOW Priority**: Incomplete vaccination series, informational items

## Integration

### Android Integration

```kotlin
// In your Android fragment/activity
private fun displayIpsContent(ipsBundle: Bundle) {
    val ipsViewer = IpsViewer()
    
    try {
        val processedIps = ipsViewer.processIpsBundle(ipsBundle)
        
        // Create Android UI components
        createPatientHeader(processedIps.patient)
        createClinicalAlerts(processedIps.alerts)
        createContentSections(processedIps.sections)
        
    } catch (e: Exception) {
        // Fallback to basic bundle display
        showBasicBundleInfo(ipsBundle)
    }
}
```

### Web Integration

The library's data models and text output can be easily adapted for web display:

```kotlin
// Generate JSON for web frontend
val processedIps = ipsViewer.processIpsBundle(ipsBundle)
val jsonData = gson.toJson(processedIps)

// Or use formatted text
val htmlContent = ipsViewer.formatIpsAsText(ipsBundle)
    .replace("\n", "<br>")
    .replace("•", "&bull;")
```

## Dependencies

- **FHIR R4**: `ca.uhn.hapi.fhir:hapi-fhir-structures-r4:6.6.0`
- **Kotlin Standard Library**: For data classes and processing
- **Java Time API**: For date handling (desugaring supported)

## Testing

The library includes comprehensive tests covering:

- IPS bundle parsing and validation
- Clinical alert generation
- Text formatting accuracy
- Error handling scenarios
- Edge cases and malformed data

Run tests with:
```bash
./gradlew :ips-viewer:test
```

## Future Enhancements

### Potential IPSViewer.com Integration

This Kotlin library is designed to potentially replace or complement the TypeScript business logic in the [IPSViewer](https://github.com/jddamore/IPSviewer) project:

1. **Kotlin/JS Compilation**: The library could be compiled to JavaScript for direct web use
2. **Shared Data Models**: Common data structures between Android and web versions
3. **Consistent Business Logic**: Same IPS interpretation rules across platforms
4. **API Compatibility**: RESTful endpoints using this library for processing

### Planned Features

- **Template-based Output**: Customizable display templates
- **Localization Support**: Multi-language clinical terminology
- **Enhanced Validation**: Deeper IPS conformance checking
- **Performance Optimization**: Large bundle handling improvements

## Contributing

The library follows standard Kotlin coding conventions and includes:

- Comprehensive documentation
- Unit tests for all public APIs
- Error handling best practices
- Clean architecture principles

## License

This library is part of the WHO Global Digital Health Certification Network (GDHCN) Validator project and follows the same licensing terms.