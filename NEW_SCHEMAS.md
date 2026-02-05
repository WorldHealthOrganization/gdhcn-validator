# How to add new schemas to the GDHCN validator.

For each new schema, there are 5 steps to go through: 
1. Choose the signing envelope (Divoc, Hcert, ICAO, SHC, etc)
2. Choose the differentiator code that will point to the new schema.
3. Code the data class for the schema in mind
4. Code structure map to convert that model into a FHIR Bundle.
5. Adapt the UI to display the contents of the new Bundle.

For the web component, step 5 is not necessary since the interface just shows the 
json payload itself on the screen.

## 1. Choose the signed envelope for the new model

Module [verify](./verify) contains the parser and verification procedures for each 
one of the available signed QR code formats: 
- Divoc, using W3C Verifiable Credentials
- HCert, using CBOR-based CWT format
- ICAO, using iJSON-based custom signature conventions
- SHC, using JWT-based signatures

Each one of them is coded in its own package and can transmit custom data models. 

## 2. Choose the differentiator 

Each envelope has it's own ways to identify the model type iside the payload. HCERT, 
for instance, uses code `-260` to identify a DCC payload and `1` to identify a WHO payload. 

## 3. Code the new class to represent the logical model

The step here simply takes the defined fields in a FHIR IG and writes them down as an object
such that a JSON parser can automatically parse the incoming payload as an object. The model
is scoped using HAPI FHIR elements to reuse parsing, but it is likely that custom parsers will 
be needed given the space limitations inside QR codes. 

### ICVP Logical Models

For International Certificate of Vaccination or Prophylaxis (ICVP), we provide several logical models:

- **`DvcLogicalModel`**: Base digital vaccine certificate model with core fields (n, dob, s, ntl, nid, ndt, gn, v)
- **`IcvpLogicalModel`**: ICVP-specific model that extends DVC with ICVP validation constraints and uses `IcvpVaccineDetails`
- **`IcvpEventLogicalModel`**: Event-based ICVP model for specific vaccination events
- **`HCertDVC`**: HCert-compatible representation of the DVC model

All models now include the `ndt` (National ID Document Type) field as required by current FSH specifications.

### Key Features

- **Type Safety**: `IcvpLogicalModel` uses `IcvpVaccineDetails` instead of generic `DvcVaccineDetails` to ensure ICVP-specific validation
- **Dynamic Product Validation**: 
  - Loads ICVP Product IDs directly from WHO SMART guidelines PreQual CodeSystem
  - HTTP client implementation to fetch from `http://smart.who.int/pcmt-vaxprequal/CodeSystem/PreQualProductIDs`
  - Graceful fallback to format validation when source is unavailable
  - Caching mechanism for performance optimization
- **Constraint Validation**: Built-in validation including:
  - "must-have-issuer-or-clinician-name" invariant  
  - National ID document type validation against HL7 v2-0203
- **Cache Management**: `refreshProductIdCache()` method for testing and cache updates
- **Error Handling**: Robust error handling for network issues and malformed responses
- **Backward Compatibility**: All constructors maintain default values for existing integrations 

## 4. Code the Mapper with Structure Maps

The mapper is a simple class that points to a Structure Map file in the IG. The structure map
should convert the logical model found in the QR code back into a FHIR Bundle.

The mapper itself is just the loader for the Structure Map.

```kotlin 
/**
 * Translates a QR CBOR object into FHIR Objects
 */
class IcvpMapper: BaseMapper() {
    fun run(icvp: IcvpLogicalModel): Bundle {
        return super.run(icvp, "DVCLMToIPS.map")
    }
}
```

### Available Structure Maps

- **`DVCLMToIPS.map`**: Converts DVC/ICVP logical models to International Patient Summary (IPS) format
- Maps `ndt` field to `Patient.identifier.type` for proper identification
- Handles ICVP-specific vaccine details with product ID validation

## 5. Adapt the interface for the new Bundle

Once the Bundle is ready the App must display its contents in a new Card. Currently the app 
only displays Vaccine and Test certificates based on a `Composition` object created by the 
Structure Map. 

After displaying the result, the app will use the IG and the bundle to evaluate a `CQL` query
called `Completed Immunization` for each `Library` available in the IG. This function name 
is hardcoded in the app and returns the immunization completion status based on the Bundle 
informatioon