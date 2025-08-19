package org.who.gdhcnvalidator.verify

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.who.gdhcnvalidator.test.BaseTrustRegistryTest
import org.who.gdhcnvalidator.verify.divoc.DivocVerifier
import org.who.gdhcnvalidator.verify.hcert.HCertVerifier
import org.who.gdhcnvalidator.verify.icao.IcaoVerifier
import org.who.gdhcnvalidator.verify.shc.ShcVerifier
import java.util.*

class QRUnpackTest: BaseTrustRegistryTest() {
    private val mapper = ObjectMapper()

    private fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    @Before
    fun setUp() {
        // fixes timezone for testing
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @Test
    fun unpackWHOQR1() {
        val qr1 = open("WHOQR1Contents.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)
        jsonEquals(open("WHOQR1Unpacked.json"), unpacked!!)
    }

    @Test
    fun unpackWHOQR2() {
        val qr2 = open("WHOQR2Contents.txt")
        val unpacked = HCertVerifier(registry).unpack(qr2)
        assertNotNull(unpacked)
        jsonEquals(open("WHOQR2Unpacked.json"), unpacked!!.replace(": undefined", ": null"))
    }

    @Test
    fun unpackSingaporePCR() {
        val qr2 = open("WHOSingaporePCRContents.txt")
        val unpacked = HCertVerifier(registry).unpack(qr2)
        assertNotNull(unpacked)
        jsonEquals(open("WHOSingaporePCRUnpacked.json"), unpacked!!.replace(": undefined", ": null"))
    }

    @Test
    fun unpackEUQR1() {
        val qr1 = open("EUQR1Contents.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)
        jsonEquals(open("EUQR1Unpacked.txt"), unpacked!!)
    }

    @Test
    fun unpackDVC1() {
        val qr1 = open("DVCTestQR.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)

        jsonEquals(open("DVCTestQRUnpacked.json"), unpacked!!)
    }

    @Test
    fun unpackDVC2() {
        val qr1 = open("DVCTest2QR.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)

        jsonEquals(open("DVCTest2QRUnpacked.json"), unpacked!!)
    }

    @Test
    fun unpackDVC3() {
        val qr1 = open("DVCTest3QR.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)

        jsonEquals(open("DVCTest3QRUnpacked.json"), unpacked!!)
    }

    @Test
    fun unpackEUItalyAcceptanceQR() {
        val qr1 = open("EUItalyAcceptanceQRContents.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)
        jsonEquals(open("EUItalyAcceptanceQRUnpacked.json"), unpacked!!)
    }

    @Test
    fun unpackEUIndonesia() {
        val qr1 = open("EUIndonesiaContents.txt")
        val unpacked = HCertVerifier(registry).unpack(qr1)
        assertNotNull(unpacked)
        jsonEquals(open("EUIndonesiaUnpacked.json"), unpacked!!)
    }

    @Test
    fun unpackSHCQR1() {
        val qr1 = open("SHCQR1Contents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCQR1Unpacked.txt"), jwt)
    }

    @Test
    fun unpackSHCSenegal() {
        val qr1 = open("SHCSenegalContents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCSenegalUnpacked.txt"), jwt)
    }

    @Test
    fun unpackSHCWAVax() {
        val qr1 = open("SHCWAVaxContents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCWAVaxUnpacked.txt"), jwt)
    }

    @Test
    fun unpackSHCTestResults() {
        val qr1 = open("SHCTestResultsContents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCTestResultsUnpacked.txt"), jwt)
    }

    @Test
    fun unpackDIVOCQR1() {
        val qr1 = open("DIVOCQR1Contents.txt")
        val jsonld = DivocVerifier(registry).unpack(qr1)
        jsonEquals(open("DIVOCQR1Unpacked.json"), jsonld!!.toJson(true))
    }

    @Test
    fun unpackDIVOCJamaica() {
        val qr1 = open("DIVOCJamaicaContents.txt")
        val jsonld = DivocVerifier(registry).unpack(qr1)
        jsonEquals(open("DIVOCJamaicaUnpacked.json"), jsonld!!.toJson(true))
    }

    @Test
    fun unpackDIVOCIndonesia() {
        val qr1 = open("DIVOCIndonesiaContents.txt")
        val jsonld = DivocVerifier(registry).unpack(qr1)
        jsonEquals(open("DIVOCIndonesiaUnpacked.json"), jsonld!!.toJson(true))
    }

    @Test
    fun unpackICAOQR1() {
        val qr1 = open("ICAOQR1Contents.txt")
        val json = IcaoVerifier(registry).unpack(qr1)
        jsonEquals(open("ICAOQR1Unpacked.json"), json)
    }
}