# Data Models Documentation

This document describes the data models used in the GDHCN Validator app for processing various types of digital health certificates.

## Overview

The GDHCN Validator supports multiple digital health certificate standards, each with its own data model. These models are parsed from QR codes and transformed into FHIR (Fast Healthcare Interoperability Resources) bundles for standardized processing.

## Base Classes

### BaseModel
**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/BaseModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/BaseModel.kt)

The foundation class for all data models, extending FHIR's `Resource` class to enable integration with the FHIR ecosystem.

### BaseMapper
**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/BaseMapper.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/BaseMapper.kt)

Base class for all mappers that transform logical models into FHIR bundles using Structure Maps.

## Supported Data Models

### 1. DDCC (Digital Documentation of COVID-19 Certificates)

DDCC is the WHO standard for digital health certificates.

#### DDCC Core Data Set - Vaccination/Immunization (VS)
**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccCoreDataSetVSModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccCoreDataSetVSModel.kt)

**Key Fields:**
- `name: StringType` - Patient name
- `birthDate: DateType` - Date of birth
- `sex: Coding` - Patient gender
- `identifier: Identifier` - Patient identifier
- `vaccination: Vaccination` - Vaccination details including:
  - `vaccine: Coding` - Vaccine type
  - `brand: Coding` - Vaccine brand
  - `manufacturer: Base` - Manufacturer information
  - `lot: StringType` - Lot number
  - `date: DateTimeType` - Vaccination date

#### DDCC Core Data Set - Test Results (TR)
**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccCoreDataSetTRModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccCoreDataSetTRModel.kt)

**Key Fields:**
- Inherits patient data from base DDCC model
- `test: DdccTestResult` - Test result details including:
  - `pathogen: Coding` - Target pathogen
  - `type: Coding` - Test type
  - `result: Coding` - Test result
  - `date: DateTimeType` - Test date

**Mapper:** [`DdccMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/ddcc/DdccMapper.kt)

### 2. EU DCC (Digital COVID Certificate)

The European Union's digital COVID certificate standard.

**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/dcc/DccModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/dcc/DccModel.kt)

#### HCertDCC
**Key Fields:**
- `ver: StringType` - Schema version
- `nam: PersonName` - Person name with standardized formats
- `dob: DateType` - Date of birth
- `v: List<Vaccination>` - Vaccination events
- `t: List<Test>` - Test events  
- `r: List<Recovery>` - Recovery events

#### Supporting Classes:
- **PersonName**: Surname and forename (both raw and standardized)
- **Vaccination**: Vaccine details with dose information
- **Test**: Test details with results
- **Recovery**: Recovery certificate information

**Mapper:** [`DccMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/dcc/DccMapper.kt)

### 3. DIVOC (Digital Infrastructure for Verifiable Open Credentialing)

India's digital health certificate system using W3C Verifiable Credentials.

**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/divoc/DivocModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/divoc/DivocModel.kt)

#### W3CVC (W3C Verifiable Credential)
**Key Fields:**
- `context: List<StringType>` - JSON-LD context
- `type: List<StringType>` - Credential types
- `issuer: StringType` - Credential issuer
- `credentialSubject: CredentialSubject` - Subject details
- `evidence: List<Evidence>` - Vaccination evidence
- `proof: Proof` - Cryptographic proof

#### CredentialSubject
Patient information including name, gender, age/DOB, nationality, and address.

#### Evidence
Vaccination details including vaccine type, manufacturer, batch, dates, and facility information.

**Mapper:** [`DivocMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/divoc/DivocMapper.kt)

### 4. SHC (Smart Health Cards)

US/International standard for health credentials using JWT format.

**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/shc/ShcModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/shc/ShcModel.kt)

#### JWTPayload
**Key Fields:**
- `iss: StringType` - Issuer
- `sub: StringType` - Subject
- `exp/nbf/iat: DateTimeType` - Token validity periods
- `vc: VC` - Verifiable credential containing FHIR bundle

#### VC (Verifiable Credential)
Contains the actual health data as a FHIR Bundle, allowing for rich, standardized health information.

**Mapper:** [`ShcMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/shc/ShcMapper.kt)

### 5. ICAO (International Civil Aviation Organization)

Standard for digital travel health documents.

**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/icao/IcaoModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/icao/IcaoModel.kt)

#### IJson
**Key Fields:**
- `data: Data` - Contains header and message
- `sig: Signature` - Digital signature

#### Key Components:
- **Header**: Issuer, type (`icao.test`, `icao.vacc`), version
- **Message**: Patient info, vaccination events, or test results
- **Patient**: Identity document details (passport, ID card, etc.)
- **VaccinationEvent**: Vaccine details with administration info
- **ServiceProvider**: Test provider information

**Mapper:** [`IcaoMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/icao/IcaoMapper.kt)

### 6. ICVP/DVC (International Certificate of Vaccination or Prophylaxis)

WHO's international vaccination certificate standard.

**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/icvp/DvcLogicalModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/icvp/DvcLogicalModel.kt)

#### DvcLogicalModel
**Key Fields:**
- `name: StringType` - Patient name
- `dob: DateType` - Date of birth
- `sex: CodeType` - Gender
- `nationality: CodeType` - Nationality
- `nid: StringType` - National identifier
- `guardian: StringType` - Guardian information
- `issuer: Reference` - Certificate issuer
- `vaccineDetails: DvcVaccineDetails` - Vaccination information

**Mapper:** [`DvcMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/icvp/DvcMapper.kt)

### 7. Smart Health Links

**Source:** [`verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/healthlink/SmartHealthLinkModel.kt`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/healthlink/SmartHealthLinkModel.kt)

Support for Smart Health Links protocol for sharing health data.

**Mapper:** [`HealthLinkMapper`](../verify/src/main/java/org/who/gdhcnvalidator/verify/hcert/healthlink/HealthLinkMapper.kt) (TODO: Implementation pending)

## Data Flow and Transformation

All models follow a consistent pattern:

1. **QR Code Parsing**: Raw QR data is decoded based on format (CBOR, JWT, JSON-LD, etc.)
2. **Model Instantiation**: Data is parsed into the appropriate logical model
3. **FHIR Transformation**: Models are transformed to FHIR bundles using Structure Maps
4. **Validation**: FHIR bundles are validated and assessed using CQL rules
5. **Display**: Results are formatted for user interface

## Common Serialization Features

- **Jackson Integration**: All models use Jackson annotations for JSON/CBOR parsing
- **Custom Deserializers**: Special handling for FHIR types and format conversions
- **FHIR Compatibility**: Models extend FHIR base classes for seamless integration

## References

- [DDCC Implementation Guide](https://worldhealthorganization.github.io/ddcc/)
- [Smart Health Cards Specification](https://spec.smarthealth.cards/)
- [EU DCC Schema](https://ec.europa.eu/health/ehealth/covid-19_en)
- [W3C Verifiable Credentials](https://www.w3.org/TR/vc-data-model/)
- [ICAO Visible Digital Seals](https://www.icao.int/Security/FAL/TRIP/Pages/Publications.aspx)
- [FHIR R4 Specification](https://hl7.org/fhir/R4/)