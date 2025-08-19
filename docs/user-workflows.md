# User Workflows Documentation

This document describes the user experience and technical workflows in the GDHCN Validator app.

## Table of Contents

- [User Journey Overview](#user-journey-overview)
- [Detailed User Workflow](#detailed-user-workflow)
  - [1. App Launch and Home Screen](#1-app-launch-and-home-screen)
  - [2. QR Code Scanning](#2-qr-code-scanning)
  - [3. QR Code Processing and Verification](#3-qr-code-processing-and-verification)
  - [4. Clinical Decision Support](#4-clinical-decision-support)
  - [5. Results Display](#5-results-display)
- [Error Handling and Edge Cases](#error-handling-and-edge-cases)
- [Technical Architecture Summary](#technical-architecture-summary)
- [Data Flow Diagram](#data-flow-diagram)

## User Journey Overview

The GDHCN Validator app provides a simple, secure way for users to verify digital health certificates. The typical user workflow involves three main steps:

1. **Open and Navigate** - User opens the app and navigates to the scanner
2. **Scan QR Code** - User points camera at a health certificate QR code  
3. **View Results** - User sees verification results and health information

## Detailed User Workflow

### 1. App Launch and Home Screen

**User Experience:**
- User opens the GDHCN Validator app
- Lands on the home screen with options to scan a QR code
- Can access settings, help, or other app features

**Technical Flow:**
- [`MainActivity`](../app/src/main/java/org/who/gdhcnvalidator/MainActivity.kt) launches
- Initializes [`FhirApplication`](../app/src/main/java/org/who/gdhcnvalidator/FhirApplication.kt) context
- Loads [`HomeFragment`](../app/src/main/java/org/who/gdhcnvalidator/views/HomeFragment.kt)
- Sets up navigation between fragments

### 2. QR Code Scanning

**User Experience:**
- User taps "Scan QR Code" button
- Camera view opens showing viewfinder
- User points camera at health certificate QR code
- App automatically detects and processes the QR code

**Technical Flow:**
- Navigates to [`ScanFragment`](../app/src/main/java/org/who/gdhcnvalidator/views/ScanFragment.kt)
- Initializes camera using Android CameraX API
- [`QRFinder`](../app/src/main/java/org/who/gdhcnvalidator/services/QRFinder.kt) continuously analyzes camera frames
- When QR code detected, extracts text content and navigates to results

**Source Code:**
```kotlin
// From ScanFragment.kt
private fun bindPreview(cameraProvider: ProcessCameraProvider) {
    val preview: Preview = Preview.Builder().build()
    val imageAnalyzer = ImageAnalysis.Builder()
        .setTargetResolution(Size(1280, 720))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    
    analyzer = QRFinder { qrContent ->
        // Navigate to results with QR content
        findNavController().navigate(
            ScanFragmentDirections.actionScanFragmentToResultFragment(qrContent)
        )
    }
    imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)
}
```

### 3. QR Code Processing and Verification

**User Experience:**
- Loading screen appears while processing
- User sees verification status (verified, invalid, etc.)
- Health certificate information displays in cards
- Trust information about the issuer is shown

**Technical Flow:**

#### 3.1 Format Detection and Decoding
[`QRDecoder`](../verify/src/main/java/org/who/gdhcnvalidator/QRDecoder.kt) determines QR format by prefix:
- `HC1:` → HCERT (EU DCC or WHO DDCC) 
- `SHC:` → Smart Health Cards
- `B64:` or `PK` → DIVOC (India)
- Contains `ICAO` → ICAO travel documents

```kotlin
// From QRDecoder.kt
fun decode(qrPayload: String): VerificationResult {
    if (qrPayload.uppercase().startsWith("HC1:")) {
        return HCertVerifier(registry).unpackAndVerify(qrPayload)
    }
    if (qrPayload.uppercase().startsWith("SHC:")) {
        return ShcVerifier(registry).unpackAndVerify(qrPayload)
    }
    // ... other formats
}
```

#### 3.2 Format-Specific Processing

Each format has its own verifier that handles:

**HCERT Verification** ([`HCertVerifier`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/HCertVerifier.kt)):
1. Base45 decode QR content
2. Decompress with zlib if needed
3. Parse CBOR/COSE structure  
4. Verify digital signature
5. Parse payload into data model (DCC, DDCC, etc.)

**SHC Verification** ([`ShcVerifier`](../verify/src/main/java/org/who/gdhcnvalidator/verify/shc/ShcVerifier.kt)):
1. Base10 decode numeric QR content
2. Parse JWT structure
3. Verify JWS signature
4. Extract FHIR bundle from payload

**DIVOC Verification** ([`DivocVerifier`](../verify/src/main/java/org/who/gdhcnvalidator/verify/divoc/DivocVerifier.kt)):
1. Base64 decode or parse plain JSON
2. Verify W3C Verifiable Credential signature
3. Parse credential subject and evidence

**ICAO Verification** ([`IcaoVerifier`](../verify/src/main/java/org/who/gdhcnvalidator/verify/icao/IcaoVerifier.kt)):
1. Parse iJSON structure
2. Verify custom signature format
3. Extract vaccination or test data

#### 3.3 Trust Verification

For all formats, [`TrustRegistry`](../trust/src/main/java/org/who/gdhcnvalidator/trust/TrustRegistry.kt):
1. Resolves issuer from signature key ID
2. Checks issuer against trusted entities list
3. Validates key status (not expired, revoked, or terminated)
4. Returns trust status and issuer information

#### 3.4 Data Model Parsing and Mapping

Each format parses into its logical data model:

- **EU DCC** → [`HCertDCC`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/dcc/DccModel.kt)
- **WHO DDCC** → [`DdccCoreDataSetVS`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccCoreDataSetVSModel.kt) or [`DdccCoreDataSetTR`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccCoreDataSetTRModel.kt)
- **DIVOC** → [`W3CVC`](../verify/src/main/java/org/who/gdhcnvalidator/verify/divoc/DivocModel.kt)
- **SHC** → [`JWTPayload`](../verify/src/main/java/org/who/gdhcnvalidator/verify/shc/ShcModel.kt)
- **ICAO** → [`IJson`](../verify/src/main/java/org/who/gdhcnvalidator/verify/icao/IcaoModel.kt)
- **ICVP** → [`DvcLogicalModel`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/icvp/DvcLogicalModel.kt)

#### 3.5 FHIR Bundle Generation

Data models are transformed to FHIR bundles using [`BaseMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/BaseMapper.kt) and Structure Maps:

```kotlin
// Example from DdccMapper
fun run(ddcc: DdccCoreDataSetVS): Bundle {
    return super.run(ddcc, "DDCCtoFHIR.map")
}
```

The Structure Maps define transformations like:
- Patient demographics
- Vaccination events → FHIR Immunization resources
- Test results → FHIR Observation resources
- Certificate metadata → FHIR Composition resource

### 4. Clinical Decision Support

**Technical Flow:**
1. FHIR bundle is saved to local database via Android FHIR Engine
2. CQL (Clinical Quality Language) libraries evaluate health status
3. [`Android Fhir Workflow`](../app/src/main/java/org/who/gdhcnvalidator/services/cql) evaluates rules like "Completed Immunization"
4. Returns assessment results and recommendations

### 5. Results Display

**User Experience:**
- [`ResultFragment`](../app/src/main/java/org/who/gdhcnvalidator/views/ResultFragment.kt) shows verification results
- Health information displays in organized cards
- Shows issuer trust status and certificate validity
- User can navigate back to scan another certificate

**Technical Flow:**
1. [`DDCCFormatter`](../app/src/main/java/org/who/gdhcnvalidator/services/DDCCFormatter.kt) converts FHIR bundle to UI cards
2. Displays patient demographics, vaccination history, test results
3. Shows verification status with appropriate colors/icons
4. Includes issuer information and trust indicators

```kotlin
// From ResultFragment.kt - Status mapping
private val statuses = mapOf(
    QRDecoder.Status.VERIFIED to R.string.verification_status_verified,
    QRDecoder.Status.INVALID_SIGNATURE to R.string.verification_status_invalid_signature,
    QRDecoder.Status.ISSUER_NOT_TRUSTED to R.string.verification_status_issuer_not_trusted,
    // ... other statuses
)
```

## Error Handling and Edge Cases

The app handles various error scenarios gracefully:

### QR Code Issues
- **Not Found**: Camera cannot detect QR code
- **Not Supported**: QR format not recognized
- **Invalid Encoding**: Malformed Base45/Base10/Base64 data

### Cryptographic Issues  
- **Invalid Signature**: Digital signature verification fails
- **Expired Keys**: Signing keys are past validity period
- **Revoked Keys**: Keys have been revoked by issuer

### Trust Issues
- **Issuer Not Trusted**: Issuer not in trusted registry
- **Terminated Keys**: Issuer has been terminated

Each error provides user-friendly messages and appropriate next steps.

## Technical Architecture Summary

```
User Scans QR → QRFinder → QRDecoder → Format-Specific Verifier
                                     ↓
FHIR Bundle ← Structure Map ← Data Model ← Parsed Payload
     ↓
Android FHIR Engine → CQL Evaluation → Assessment Results
     ↓
DDCCFormatter → UI Cards → ResultFragment → User
```

## Data Flow Diagram

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ HomeFragment│ →  │ ScanFragment│ →  │ResultFragment│
└─────────────┘    └─────────────┘    └─────────────┘
                          │                   ↑
                    ┌─────▼─────┐             │
                    │ QRFinder  │             │
                    └─────┬─────┘             │
                          │                   │
                    ┌─────▼─────┐             │
                    │ QRDecoder │             │
                    └─────┬─────┘             │
                          │                   │
         ┌────────────────┼────────────────┐  │
         │                │                │  │
   ┌─────▼─────┐ ┌────────▼────────┐ ┌─────▼──▼──┐
   │HCertVerifier│ │  ShcVerifier   │ │DivocVerifier│
   └─────┬─────┘ └────────┬────────┘ └─────┬─────┘
         │                │                │
   ┌─────▼─────┐ ┌────────▼────────┐ ┌─────▼─────┐
   │ DccModel  │ │   JWTPayload    │ │  W3CVC    │
   └─────┬─────┘ └────────┬────────┘ └─────┬─────┘
         │                │                │
   ┌─────▼─────┐ ┌────────▼────────┐ ┌─────▼─────┐
   │ DccMapper │ │   ShcMapper     │ │DivocMapper│
   └─────┬─────┘ └────────┬────────┘ └─────┬─────┘
         │                │                │
         └────────────────┼────────────────┘
                          │
                    ┌─────▼─────┐
                    │FHIR Bundle│
                    └─────┬─────┘
                          │
                 ┌────────▼────────┐
                 │Android FHIR Engine│
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │  CQL Evaluation │
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │ DDCCFormatter   │
                 └─────────────────┘
```

This workflow ensures secure, standardized processing of multiple health certificate formats while providing a smooth user experience.