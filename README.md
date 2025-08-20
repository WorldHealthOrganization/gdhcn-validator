# World Health Organization's GDHCN Verifier App

<img align="right" src="./docs/screenshots/3.Results.png" data-canonical-src="./docs/screenshots/3.Results.png" width="350px"/>

Digital Health Certificates verifier app for Android. The app scans a QR code for a credential/pass, 
cryptographically verifies it and displays the results on the phone. No information is transmitted 
anywhere. Our goal is to make a Verifier App with the widest possible verification capabilities.

# Current Features

1. Decodes QR Codes
2. Cryptographically Verifies the information following the specifications of
   1. W3C VC
   2. Smart Health Cards 
   3. EU DCC, WHO DDCC and LAC PASS DCC 
   4. ICAO Visible Digital Seals
3. Verifies the issuer's trust using a [DID-Based](https://www.w3.org/TR/did-core/) Trust List from the [Global Digital Health Certification Network](https://www.who.int/initiatives/global-digital-health-certification-network)
4. Transform the QR Payload using [FHIR Structure Maps](https://worldhealthorganization.github.io/ddcc/) for [International
   Certificate of Vaccination of Prophylaxis] (https://worldhealthorganization.github.io/smart-icvp/artifacts.html) and [International Patient Summary](https://hl7.org/fhir/uv/ips/)
5. Calculates the assessment of the health information using CQL Libraries from subscribed IGs
6. Displays the medical information, the credential information, the issuer information and the assessment results in the screen.

## Documentation

### Project Documentation
- [**Data Models**](docs/data-models.md) - Comprehensive documentation of all supported certificate data models (DDCC, DCC, DIVOC, SHC, ICAO, ICVP)
- [**User Workflows**](docs/user-workflows.md) - User experience and technical workflow documentation
- [**Deployment Guide**](docs/deployment.md) - Complete guide for preview branch and production deployments
- [**Adding New Schemas**](NEW_SCHEMAS.md) - Guide for adding support for new certificate formats

### Reference Documentation
- https://worldhealthorganization.github.io/smart-trust/
- https://worldhealthorganization.github.io/smart-icvp
- https://worldhealthorganization.github.io/ddcc/
- https://smart.who.int/trust/

# Development Overview
- 
## Setup

Make sure to have the following pre-requisites installed:
1. Java 17
2. Android Studio Koala+
3. Android 7.0+ Phone or Emulation setup

Fork and clone this repository and import into Android Studio
```bash
git clone https://github.com/WorldHealthOrganization/gdhcn-validator.git
```

Use one of the Android Studio builds to install and run the app in your device or a simulator.

## Building
Build the app:
```bash
./gradlew assembleDebug
```

## Testing
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Installing on device
```bash
./gradlew installDebug
```

## KeyCloak instructions

Follow server setup [here](https://www.keycloak.org/getting-started/getting-started-zip): 
1. Download Keycloak
2. Start Keycloak
3. Create an admin user
4. Login to the admin console
5. Create a realm
6. Create a user (which we will connect from the app)
8. Create a client with: 
  - Client Id: verifier-app 
  - Client Protocol: openid-connect
  - Redirect URIs: org.who.gdhcnverifier:/redirect

Start keycloak with the local network's IP a phone can reach:
```bash
bin/standalone.sh -b <YOUR LOCAL IP>
```

Android will connect with your local IP. Just make sure the phone is in the same WIFI as the dev's computer

## Screen + Class flow overview

```
┌──────────────────────────────────────────────────┐      ┌────────────────┐   ┌────────────┐
│                  MainActivity                    │      │ TrustRegistry  │   │ IgRegistry │
└──────────────────────────────────────────────────┘      └─────────────╥──┘   └─────╥──────┘
┌──────────────┐ ┌──────────────┐ ┌────────────────┐                    ║            ║
│ HomeFragment ├→┤ ScanFragment ├→┤ ResultFragment │←─GDHCN UI Card─────╫─────────┐  ║
└──────────────┘ └─────┬──▲─────┘ └────────┬───────┘                    ║         │  ║
                  Image│  │QRContent       │QRContent                   ║         │  ║
                 ┌─────▼──┴─────┐     ┌────▼───────┐                    ║         │  ║
                 │   QRFinder   │     │ QRDecoder  │         IssuerData ║         │  ║
                 └──────────────┘     └────┬───────┘         PublicKeys ║         │  ║
                                           │QRContent                   ║         │  ║
            ┌─────────────────┬────────────┴─────┬───────────────────┐  ║         │  ║
 ╔══════════╪═════════════════╪══════════════════╪═══════════════════╪══╩══════╗  │  ║
 ║ ┌────────▼───────┐  ┌──────▼──────┐   ┌───────▼───────┐   ┌───────▼───────┐ ║  │  ║
 ║ │  HCertVerifier │  │ ShcVerifier │   │ DivocVerifier │   │ IcaoVerifier  │ ║  │  ║
 ║ └────┬───────────┘  └──────┬──────┘   └───────┬───────┘   └───────┬───────┘ ║  │  ║
 ╚══════╪═════════════════════╪══════════════════╪═══════════════════╪═════════╝  │  ║
        │HCERT CBOR           │JWT JSON          │JSONLD W3C VC      │iJSON       │  ║
   ┌────▼───────────┐  ┌──────▼──────┐    ┌──────▼──────┐     ┌──────▼──────┐     │  ║
   │   CBORParser   │  │ JSON Parser │    │ JSON Parser │     │ JSON Parser │     │  ║
   └──┬──────────┬──┘  └──────┬──────┘    └──────┬──────┘     └──────┬──────┘     │  ║
      │WhoModel  │DccModel    │ShcModel          │DivocModel         │IcaoModel   │  ║
╔═════╪══════════╪════════════╪══════════════════╪═══════════════════╪═════════╗  │  ║ StructureMaps
║ ┌───▼───────┐┌─▼─────────┐┌─▼─────────┐  ┌─────▼───────┐ ┌─────────▼───────┐ ║  │  ║ 
║ │ DDCCMapper││ DCCMapper ││ JWTMapper │  │ DivocMapper │ │   IJsonMapper   │ ║══│══╝
║ └───┬───────┘└──┬────────┘└─┬─────────┘  └─────┬───────┘ └─────────┬───────┘ ║  │  ║
╚═════╪═══════════╪═══════════╪══════════════════╪═══════════════════╪═════════╝  │  ║
      └───────────┴───────────┴────────────┬─────┴───────────────────┘            │  ║
                                           │FHIR Bundle                           │  ║
                                 ┌─────────▼───────────┐                          │  ║
                                 │ Android Fhir Engine │                          │  ║
                                 │  (Save to Database) │                          │  ║
                                 └─────────┬───────────┘                          │  ║
                                           │Patient ID                            │  ║
                 ┌─────────────────────┬───┴────────────────────┐                 │  ║
   ╔═════════════╪═════════════════════╪════════════════════════╪══════════════╗  │  ║                 
   ║   ┌─────────▼──────────┐┌─────────▼──────────┐   ┌─────────▼──────────┐   ║  │  ║  
   ║   │    Compile IG #1   ││    Compile IG #2   │...│    Compile IG #n   │   ║  │  ║ CQL Libraries 
   ║   │ (Assessment Rules) ││ (Assessment Rules) │   │ (Assessment Rules) │   ║══│══╝
   ║   └─────────┬──────────┘└─────────┬──────────┘   └─────────┬──────────┘   ║  │    
   ╚═════════════╪═════════════════════╪════════════════════════╪══════════════╝  │                
                 └─────────────────────┴───┬────────────────────┘                 │                                                                               
                                           │Patient ID, Rule ID                   │
                                 ┌─────────▼──────────────┐                       │
                                 │  Android Fhir Workflow │                       │
                                 │   (Evaluate Status)    │                       │  
                                 └─────────┬──────────────┘                       │
                                           │Fhir Composite, Status                │
                                    ┌──────▼─────────┐                            │
                                    │    Formatter   ├→─ UI Card ─────────────────┘
                                    └────────────────┘
```

## How to Deploy

> **📖 For comprehensive deployment documentation, see the [Deployment Guide](docs/deployment.md)**

### Quick Start - Production Release

1. Generate a new signing key 
```
keytool -genkey -v -keystore <my-release-key.keystore> -alias <alias_name> -keyalg RSA -keysize 2048 -validity 10000
```
2. Create 4 Secret Key variables on your GitHub repository and fill in with the signing key information
   - `KEY_ALIAS` <- `<alias_name>`
   - `KEY_PASSWORD` <- `<your password>`
   - `KEY_STORE_PASSWORD` <- `<your key store password>`
   - `SIGNING_KEY` <- the data from `<my-release-key.keystore>`
3. Change the `versionCode` and `versionName` on `app/build.gradle`
4. Commit and push. 
5. Tag the commit with `v{x.x.x}`
6. Let the [Create Release GitHub Action](https://github.com/WorldHealthOrganization/gdhcn-validator/actions/workflows/create-release.yml) build a new `aab` file. 
7. Add your CHANGE LOG to the description of the new release
8. Download the `aab` file and upload it to the PlayStore.

### Preview Branch Deployment

For testing changes before production:

- **Pull Requests**: Automatic preview deployment with artifacts and optional live preview
- **Feature Branches**: Push to `preview/*` or `feature/*` branches for manual deployment
- **Main Branch**: Automatic staging deployment on every push to main

See the [Deployment Guide](docs/deployment.md) for detailed instructions on preview deployments, configuration options, and troubleshooting. 

# Contributing

[Issues](https://github.com/WorldHealthOrganization/gdhcn-validator/issues) and [pull requests](https://github.com/WorldHealthOrganization/gdhcn-validator/pulls) are very welcome.

# License

Copyright 2021 PathCheck Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.