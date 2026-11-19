# GitHub Copilot Instructions for GDHCN Validator

This repository is the **WHO Global Digital Health Certification Network (GDHCN) Validator** - an Android app for verifying digital health certificates.

## Quick Repository Overview

### Purpose
- Verify digital health certificates (vaccination, test results) from QR codes
- Support multiple international standards (WHO DDCC, EU DCC, Smart Health Cards, DIVOC, ICAO)
- Cryptographically verify signatures and check issuer trust
- Display health information in standardized format using FHIR

### Architecture
- **Android App** (`app/`) - UI, camera scanning, results display
- **Verification Module** (`verify/`) - QR decoding, signature verification, data parsing
- **Trust Registry** (`trust/`) - Issuer verification and key management
- **Web Interface** (`web/`) - Web-based validator version

### Key Technologies
- **Android** with Kotlin
- **FHIR R4** for health data interoperability
- **Structure Maps** for data transformation
- **CQL (Clinical Quality Language)** for health assessments
- **Multiple cryptographic standards** (CBOR/COSE, JWT/JWS, W3C VC)

## Data Models Location Guide

All data models are in `verify/src/main/java/org/who/gdhcnvalidator/verify/`:

### WHO DDCC (Digital Documentation of COVID-19 Certificates)
```
hcert/ddcc/
├── DdccCoreDataSet.kt          # Base DDCC model
├── DdccCoreDataSetVSModel.kt   # Vaccination/Immunization
├── DdccCoreDataSetTRModel.kt   # Test Results
└── DdccMapper.kt               # FHIR transformation
```

### EU DCC (Digital COVID Certificate)
```
hcert/dcc/
├── DccModel.kt                 # EU DCC data model
└── DccMapper.kt                # FHIR transformation
```

### DIVOC (India's Digital Health Certificate)
```
divoc/
├── DivocModel.kt               # W3C Verifiable Credential model
├── DivocMapper.kt              # FHIR transformation
└── DivocVerifier.kt            # Signature verification
```

### Smart Health Cards (US/International)
```
shc/
├── ShcModel.kt                 # JWT-based model with FHIR bundle
├── ShcMapper.kt                # FHIR transformation
└── ShcVerifier.kt              # JWT signature verification
```

### ICAO (Travel Documents)
```
icao/
├── IcaoModel.kt                # iJSON format for travel
├── IcaoMapper.kt               # FHIR transformation
└── IcaoVerifier.kt             # Custom signature verification
```

### ICVP (International Certificate of Vaccination)
```
hcert/icvp/
├── DvcLogicalModel.kt          # WHO vaccination certificate
└── DvcMapper.kt                # FHIR transformation
```

## User Workflow Code Locations

### UI Components
```
app/src/main/java/org/who/gdhcnvalidator/
├── MainActivity.kt             # Main app entry point
└── views/
    ├── HomeFragment.kt         # Landing screen
    ├── ScanFragment.kt         # QR code scanning
    └── ResultFragment.kt       # Results display
```

### Core Processing
```
├── QRDecoder.kt                # Format detection and routing
├── services/
│   ├── QRFinder.kt             # Camera QR detection
│   └── DDCCFormatter.kt        # FHIR to UI conversion
└── verify/[format]/[Format]Verifier.kt  # Format-specific verification
```

## Common Development Patterns

### Adding New Certificate Format
1. Create model in `verify/[format]/[Format]Model.kt`
2. Create mapper in `verify/[format]/[Format]Mapper.kt` 
3. Create verifier in `verify/[format]/[Format]Verifier.kt`
4. Add format detection to `QRDecoder.kt`
5. Update UI formatter if needed

### Data Model Structure
- All models extend `BaseModel` (FHIR Resource)
- Use Jackson annotations for JSON/CBOR parsing
- FHIR types for interoperability (`StringType`, `DateType`, etc.)

### Verification Flow
1. **Format Detection** - QR prefix determines verifier
2. **Cryptographic Verification** - Signature validation
3. **Trust Verification** - Issuer lookup in trust registry  
4. **Data Parsing** - QR content → Data Model
5. **FHIR Transformation** - Data Model → FHIR Bundle via Structure Maps
6. **Clinical Assessment** - CQL rules evaluate health status
7. **UI Display** - FHIR Bundle → UI cards

### Testing
- Unit tests in `verify/src/test/`
- Android tests in `app/src/androidTest/`
- Test resources in `test-resources/`

## Important Files for Contributors

### Documentation
- [`docs/data-models.md`](docs/data-models.md) - Comprehensive data model documentation
- [`docs/user-workflows.md`](docs/user-workflows.md) - User experience and technical flows
- [`NEW_SCHEMAS.md`](NEW_SCHEMAS.md) - Guide for adding new certificate formats

### Configuration
- [`settings.gradle`](settings.gradle) - Module configuration
- [`build.gradle`](build.gradle) - Dependencies and build config
- [`app/build.gradle`](app/build.gradle) - Android app configuration

### Key Directories to Know
- `verify/` - Core verification logic (most development happens here)
- `trust/` - Trust registry and issuer verification
- `app/src/main/java/` - Android UI and services
- `web/` - Web-based validator (Spring Boot)

## Development Tips

### When Working with Data Models
- Follow existing patterns in `BaseModel` and `BaseMapper`
- Use FHIR types for health data compatibility
- Add proper Jackson deserializers for complex fields
- Test with real QR code samples in `test-resources/`

### When Working with Verification
- Each format needs its own verifier extending verification patterns
- Signature verification must check key status in trust registry
- Error handling should map to `QRDecoder.Status` enum values

### When Working with UI
- Follow Material Design patterns used in existing fragments
- Use data binding for view updates
- Handle verification states properly (loading, error, success)

### When Working with FHIR
- Structure Maps define transformations in `verify/src/main/resources/`
- CQL libraries handle clinical assessments
- Android FHIR Engine manages local storage and evaluation

This codebase is well-structured with clear separation of concerns. Most certificate format work happens in the `verify/` module, while UI and user experience code is in the `app/` module.