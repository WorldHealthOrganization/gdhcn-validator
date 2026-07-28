# Sample QR Codes

Sample credentials kept in `docs/` for manual testing of the Android app and the
[web verifier](../web/README.md). The prefix records which trust environment issued them:

- `DEMO-` — issued against the test/development trust lists
- `PROD-` — issued by production issuers

Naming follows `{SCOPE}-{FORMAT}-{QUALIFIER}.png`.

## PH4H samples

Signed by the `XCL` test country on the GDHCN **development** trust list
(`did:web:tng-cdn-dev.who.int:trustlist`). They verify only for as long as that key
remains published there.

| File | Format | Notes |
|------|--------|-------|
| [DEMO-VHL-QR1.png](DEMO-VHL-QR1.png) | Verifiable Health Link (claim `5`) | **Passcode `1234`.** Resolves to an IPS document (Patient, Condition, AllergyIntolerance) fetched from the issuer's manifest server. |
| [DEMO-ICVP-QR1.png](DEMO-ICVP-QR1.png) | ICVP (claim `-6`) | Yellow fever vaccination, mapped to an International Patient Summary |
| [DEMO-MEOW-QR1.png](DEMO-MEOW-QR1.png) | Medication Overview (claim `-7`) | Losartan 50 mg treatment line, mapped to an IHE Medication Overview Bundle |

Two caveats for the VHL sample:

- Its content is fetched over the network from the issuer's server, so it only works while
  that server is reachable. The other samples verify entirely offline apart from the trust list.
- The link carries an `exp` of 2026-07-10, which has passed. The issuer's server still serves
  the manifest, and the verifier does not currently reject expired links, so the sample keeps
  working — do not read a successful verification here as evidence that expiry is enforced.

## Other samples

| File | Format |
|------|--------|
| [DEMO-DDCC-QR1.png](DEMO-DDCC-QR1.png), [DEMO-DDCC-QR2.png](DEMO-DDCC-QR2.png) | WHO DDCC |
| [DEMO-SHC-QR1.png](DEMO-SHC-QR1.png) | Smart Health Card |
| [DEMO-DIVOC-JAMAICA.png](DEMO-DIVOC-JAMAICA.png), [PROD-DIVOC-INDIA.png](PROD-DIVOC-INDIA.png) | DIVOC |
| [PROD-EU-QR1.png](PROD-EU-QR1.png) | EU DCC |
| [PROD-ICAO-Australia.png](PROD-ICAO-Australia.png) | ICAO Visible Digital Seal |

## Verifying a sample

With the [web verifier](../web/README.md) running, upload the image at
http://localhost:8080/index, or post it to the API:

```bash
curl --form file=@docs/DEMO-ICVP-QR1.png http://localhost:8080/findAndVerify
```

For the VHL sample the first response is `VHL_REQUIRES_PIN`; enter the passcode in the form,
or repeat the call against `/verify` with `{"uri": "HC1:...", "pin": "1234"}`.
