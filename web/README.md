# World Health Organization's Web GDHCN Verifier

This module is a proof of concept UI and API for a web-based GDHCN Verifier

# Usage

The API has two available REST inputs: 

## 1. Image input with 

```bash
curl --form file='@QRCode.png' https://localhost:8080/findAndVerify
```

## 2. QR Content in a POST with JSON

```bash
curl -X POST --location "https://localhost:8080/verify" -H "Content-Type: application/json" -d "@QRCodeIn.json"
```

where QRCodeIn.json contains the string representation inside the QR Code. For example 

```json
{
"uri": "HC1:6BF6W1SX77XS%20KHH3QH0 8KQLRM86427%NFPT*N2ZNC1R6A-LGN84TEZPF5DQPL9V:JNJMQ82$ OJU3%2SUXT Y1I-0Z+GC:UW+12YE3L3PE7E8TK5MNBPGEVQR3J:DDJL:/SQ0V9L7U6IS2MQF9-Q16UG$QVW6C8AK9FHVUD%RNM3G9$EERHCVAY0QZJ8ZEA1UG4GFJ LIG4ABGKMKE1TO58NP8G50IAIF8D55RXOQNYH4ZD4YQZZIFF6+9B-DBUFGW/B S3K-OQ+11YMCE42998UII7CICK 8M398XQNWBT5DB5 C0P8 :NZ372DB3UGYVH7KSI-RKYLUNH43DL*2YR5+-DVNAFKGW+13%R+POHWBB42B:2M59YNR0B0%VODGQ+SGF1M* AUKKKSI6G43QBA/O+GH1 2Z%M200.D1%+QWZ14HC*MT6.KC8M%2LKDVV*NS*SM$8*NQ/MCYRC70CV$BWQL%V28NJEYUG*N7PGQGDS+F0:BH2GS0BEXBEA6+/GZYQPK2B2AQWNYNPZ*N8DSK/V%LFS8VIDSM*36EV JSO0T%5FCGW9US8BQV$VH QE2GZ%U75EEJH"
}
```

Users can either pass the picture to find the QR Code or find the QR code themselves first and then pass the information inside it along. 

The request also accepts an optional `pin`, used to unlock passcode-protected
Verifiable Health Links (see below):

```json
{
  "uri": "HC1:6BFOXN...",
  "pin": "1234"
}
```

## 3. Verifiable Health Links

When the QR carries a Verifiable Health Link, the signed payload only references the health
data. The signature and the issuer are verified first; the content is then fetched from the
issuer's server.

If the link is passcode-protected, the first call returns `VHL_REQUIRES_PIN` together with a
`vhlInfo` object describing the link:

```json
{
  "status" : "VHL_REQUIRES_PIN",
  "issuer" : { "...": "..." },
  "vhlInfo" : {
    "decodedLink" : {
      "url" : "https://example.org/v2/manifests/01f0a8e5-1e94-4ec4-9b31-ac96b208f126",
      "flag" : "P",
      "label" : "GDHCN Validator"
    },
    "requiresPin" : true
  }
}
```

Repeat the request with the `pin` field to retrieve the content. On success the status is
`VERIFIED` and `contents` holds the FHIR document fetched from the manifest. `VHL_FETCH_ERROR`
means the manifest could not be retrieved — most often an incorrect passcode.

In the web UI the same flow is handled by a passcode form on the results page.

> **Note:** `vhlInfo.decodedLink` may include the link's decryption `key`. This is inherent to
> the SMART Health Links model, but keep it in mind before exposing this API to untrusted clients.

## 4. The output

The output includes stage-by-stage information of the verification process:

- "status" -> Error codes defined [here](https://github.com/WorldHealthOrganization/gdhcn-validator/blob/main/verify/src/main/java/org/who/gdhcnvalidator/QRDecoder.kt)
- "qr" -> the value in the QR. if the QR is binary (DIVOC), it outputs a Base64 of the binary content. 
- "unpacked" -> the best representation of the contents as expected by each specification
- "contents" -> the resulting FHIR Composition 
- "issuer" -> the issuer of the keys from the DID Document

```json
{
  "status" : "VERIFIED",
  "qr" : "HC1:6BF6W1SX77XS%20KHH3QH0 8KQLRM86427%NFPT*N2ZNC1R6A-LGN84TEZPF5DQPL9V:JNJMQ82$ OJU3%2SUXT Y1I-0Z+GC:UW+12YE3L3PE7E8TK5MNBPGEVQR3J:DDJL:/SQ0V9L7U6IS2MQF9-Q16UG$QVW6C8AK9FHVUD%RNM3G9$EERHCVAY0QZJ8ZEA1UG4GFJ LIG4ABGKMKE1TO58NP8G50IAIF8D55RXOQNYH4ZD4YQZZIFF6+9B-DBUFGW/B S3K-OQ+11YMCE42998UII7CICK 8M398XQNWBT5DB5 C0P8 :NZ372DB3UGYVH7KSI-RKYLUNH43DL*2YR5+-DVNAFKGW+13%R+POHWBB42B:2M59YNR0B0%VODGQ+SGF1M* AUKKKSI6G43QBA/O+GH1 2Z%M200.D1%+QWZ14HC*MT6.KC8M%2LKDVV*NS*SM$8*NQ/MCYRC70CV$BWQL%V28NJEYUG*N7PGQGDS+F0:BH2GS0BEXBEA6+/GZYQPK2B2AQWNYNPZ*N8DSK/V%LFS8VIDSM*36EV JSO0T%5FCGW9US8BQV$VH QE2GZ%U75EEJH",
  "unpacked" : "{\"1\":\"CL\",\"4\":1681430400,\"6\":1653927539,\"-260\":{\"1\":{\"v\":[{\"dn\":2,\"ma\":\"Sinovac-Biotech\",\"vp\":\"1119305005\",\"dt\":\"2022-04-14\",\"co\":\"CL\",\"ci\":\"URN:UVCI:V1:CL:8KYL4SKUQXIWYSAU97KX49XVJV\",\"mp\":\"CoronaVac\",\"is\":\"Ministerio de Salud\",\"sd\":2,\"tg\":\"840539006\"}],\"nam\":{\"fnt\":\"MARIA CARMEN DE LOS ANGELES\",\"fn\":\"Maria Carmen De los angeles\",\"gnt\":\"DEL RIO\",\"gn\":\"Del rio\"},\"ver\":\"1.3.0\",\"dob\":\"1989-12-14\"}}}",
  "contents" : "{\"resourceType\":\"Composition\",\"contained\":[{\"resourceType\":\"Patient\",\"id\":\"1\",\"name\":[{\"use\":\"official\",\"family\":\"Maria Carmen De los angeles\",\"given\":[\"Del rio\"]},{\"use\":\"official\",\"family\":\"MARIA CARMEN DE LOS ANGELES\",\"given\":[\"DEL RIO\"]}],\"birthDate\":\"1989-12-14\"},{\"resourceType\":\"Immunization\",\"id\":\"2\",\"extension\":[{\"url\":\"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand\",\"valueCoding\":{\"system\":\"https://www.ema.europa.eu/en/medicines/human/EPAR/comirnaty\",\"code\":\"CoronaVac\"}},{\"url\":\"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization\",\"valueCoding\":{\"code\":\"Sinovac-Biotech\"}},{\"url\":\"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination\",\"valueCoding\":{\"system\":\"urn:iso:std:iso:3166\",\"code\":\"CL\"}}],\"identifier\":[{\"value\":\"URN:UVCI:V1:CL:8KYL4SKUQXIWYSAU97KX49XVJV\"}],\"vaccineCode\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"1119305005\"}]},\"patient\":{\"reference\":\"#1\"},\"occurrenceDateTime\":\"2022-04-14\",\"manufacturer\":{\"id\":\"Sinovac-Biotech\"},\"protocolApplied\":[{\"authority\":{\"reference\":\"#3\"},\"targetDisease\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"840539006\"}]}],\"doseNumberPositiveInt\":2,\"seriesDosesPositiveInt\":2}]},{\"resourceType\":\"Organization\",\"id\":\"3\",\"identifier\":[{\"value\":\"Ministerio de Salud\"}]}],\"type\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"82593-5\",\"display\":\"Immunization summary report\"}]},\"category\":[{\"coding\":[{\"code\":\"ddcc-vs\"}]}],\"subject\":{\"reference\":\"#1\"},\"author\":[{\"reference\":\"#3\"}],\"title\":\"International Certificate of Vaccination or Prophylaxis\",\"event\":[{\"period\":{\"start\":\"2022-05-30\",\"end\":\"2023-04-13\"}}],\"section\":[{\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"11369-6\",\"display\":\"History of Immunization Narrative\"}]},\"author\":[{\"reference\":\"#3\"}],\"entry\":[{\"reference\":\"#2\"}]}]}",
  "issuer" : {
    "displayName" : {
      "en" : "Gov of Chile"
    },
    "displayLogo" : "",
    "status" : "CURRENT",
    "scope" : "ACCEPTANCE_TEST",
    "validFrom" : "2022-05-06T23:01:23.000+00:00",
    "validUntil" : "2024-05-05T23:01:23.000+00:00",
    "publicKey" : "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsG7Rt8Zs7NzNAGoCmuJJAdoJgdN5\na565v+/I0HMUPdYrzwwzE996cB6oSnryESkSZN3+Zxykq3C6M8hio+ov+Q==\n-----END PUBLIC KEY-----\n"
  }
}
```


# Development Overview

## Setup

Make sure to have the following pre-requisites installed:
1. Java 21 (this module and the `verify`/`trust` modules build on a Java 21 toolchain)
2. Android Studio Koala+ — only needed to work on the Android app in `app/`

Fork and clone this repository
```bash
git clone https://github.com/WorldHealthOrganization/gdhcn-validator.git
```

## Building and Running
Start the server:
```bash
./gradlew :web:bootRun
```

It will start spring boot server and run on http://localhost:8080

Without the Android SDK installed, add `--configure-on-demand` so Gradle does not configure
the `app` module:
```bash
./gradlew :web:bootRun --configure-on-demand
```

## Testing
```bash
./gradlew :web:test
```