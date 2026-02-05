package org.who.gdhcnvalidator.verify.hcert.icvp

import org.hl7.fhir.r4.model.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for updated ICVP logical models to verify FSH compliance
 */
class IcvpLogicalModelTest {

    @Test
    fun testDvcLogicalModelWithNdt() {
        // Test that DvcLogicalModel can be created with the new ndt field
        val model = DvcLogicalModel(
            name = StringType("John Doe"),
            dob = DateType("1990-01-01"),
            sex = CodeType("M"),
            nationality = CodeType("US"),
            nid = StringType("123456789"),
            ndt = CodeType("passport"), // New field
            guardian = StringType("Jane Doe"),
            issuer = Reference("Organization/1"),
            vaccineDetails = DvcVaccineDetails()
        )
        
        assertNotNull(model)
        assertEquals("passport", model.ndt?.value)
        assertEquals("John Doe", model.name?.value)
    }

    @Test
    fun testIcvpLogicalModelWithNdt() {
        // Test that IcvpLogicalModel can be created with the new ndt field
        val vaccineDetails = IcvpVaccineDetails(
            doseNumber = CodeableConcept(),
            disease = Coding(),
            vaccineClassification = CodeableConcept(),
            vaccineTradeItem = StringType("YellowFeverProductd2c75a15ed309658b3968519ddb31690"),
            date = DateTimeType(),
            clinicianName = StringType("Dr. Smith"),
            issuer = Reference("Organization/1"),
            manufacturerId = Identifier(),
            manufacturer = StringType("Test Manufacturer"),
            batchNo = StringType("BATCH123"),
            validityPeriod = Period()
        )
        
        val model = IcvpLogicalModel(
            name = StringType("John Doe"),
            dob = DateType("1990-01-01"),
            sex = CodeType("M"),
            nationality = CodeType("US"),
            nid = StringType("123456789"),
            ndt = CodeType("passport"), // New field
            guardian = StringType("Jane Doe"),
            issuer = Reference("Organization/1"),
            vaccineDetails = vaccineDetails
        )
        
        assertNotNull(model)
        assertEquals("passport", model.ndt?.value)
        assertEquals("John Doe", model.name?.value)
    }

    @Test
    fun testHCertDVCWithNdt() {
        // Test that HCertDVC model supports the new ndt field
        val hcertModel = HCertDVC(
            n = StringType("John Doe"),
            dob = DateType("1990-01-01"),
            s = CodeType("M"),
            ntl = CodeType("US"),
            nid = StringType("123456789"),
            ndt = CodeType("passport"), // New field
            gn = StringType("Jane Doe"),
            v = DvcHCertVaccination(
                extension = null,
                modifierExtension = null,
                dn = CodeType("1"),
                tg = CodeType("disease"),
                vp = CodeType("vaccine"),
                mp = IdType("vaccine-id"),
                ma = StringType("manufacturer"),
                mid = IdType("manufacturer-id"),
                dt = DateType("2023-01-01"),
                bo = StringType("batch"),
                vls = null,
                vle = null,
                cn = StringType("Dr. Smith"),
                `is` = StringType("issuer")
            )
        )
        
        assertNotNull(hcertModel)
        assertEquals("passport", hcertModel.ndt?.value)
    }

    @Test
    fun testVaccineDetailsValidation() {
        val vaccineDetails = IcvpVaccineDetails(
            doseNumber = CodeableConcept(),
            disease = Coding(),
            vaccineClassification = CodeableConcept(),
            vaccineTradeItem = StringType("validProductId"),
            date = DateTimeType(),
            clinicianName = StringType("Dr. Smith"), // Has clinician name
            issuer = null, // No issuer
            manufacturerId = Identifier(),
            manufacturer = StringType("Test Manufacturer"),
            batchNo = StringType("BATCH123"),
            validityPeriod = Period()
        )
        
        val errors = vaccineDetails.validateIcvpConstraints()
        assertTrue("Should pass validation with clinician name", errors.isEmpty())
    }

    @Test
    fun testVaccineDetailsValidationFailure() {
        val vaccineDetails = IcvpVaccineDetails(
            doseNumber = CodeableConcept(),
            disease = Coding(),
            vaccineClassification = CodeableConcept(),
            vaccineTradeItem = StringType(""),
            date = DateTimeType(),
            clinicianName = null, // No clinician name
            issuer = null, // No issuer
            manufacturerId = Identifier(),
            manufacturer = StringType("Test Manufacturer"),
            batchNo = StringType("BATCH123"),
            validityPeriod = Period()
        )
        
        val errors = vaccineDetails.validateIcvpConstraints()
        assertFalse("Should fail validation without issuer or clinician name", errors.isEmpty())
        assertTrue("Should contain invariant error", 
            errors.any { it.contains("Either issuer or clinicianName must be present") })
    }

    @Test
    fun testDvcLogicalModelValidation() {
        val model = DvcLogicalModel(
            name = StringType("John Doe"),
            dob = DateType("1990-01-01"),
            sex = CodeType("M"),
            nationality = CodeType("US"),
            nid = StringType("123456789"),
            ndt = CodeType("PPN"), // Valid passport document type
            guardian = StringType("Jane Doe"),
            issuer = Reference("Organization/1"),
            vaccineDetails = DvcVaccineDetails()
        )
        
        val errors = model.validateIcvpConstraints()
        assertTrue("Should pass validation with valid ndt", errors.isEmpty())
    }

    @Test
    fun testNationalIdDocumentTypeValidation() {
        // Test with valid ndt
        assertTrue("PPN should be valid", IcvpValidation.validateNationalIdDocumentType("PPN"))
        assertTrue("DL should be valid", IcvpValidation.validateNationalIdDocumentType("DL"))
        assertTrue("null should be valid (optional)", IcvpValidation.validateNationalIdDocumentType(null))
        assertTrue("empty should be valid (optional)", IcvpValidation.validateNationalIdDocumentType(""))
        
        // Test edge cases
        assertTrue("Short codes should be valid", IcvpValidation.validateNationalIdDocumentType("XX"))
    }

    @Test
    fun testIcvpProductIdValidation() {
        // Reset cache to test fresh loading
        IcvpValidation.refreshProductIdCache()
        
        // Test with valid format product IDs (alphanumeric, longer than 10 chars)
        assertTrue("Should accept properly formatted Yellow Fever product", 
            IcvpValidation.validateIcvpProductId("YellowFeverProductd2c75a15ed309658b3968519ddb31690"))
        assertTrue("Should accept properly formatted Polio product", 
            IcvpValidation.validateIcvpProductId("PolioVaccineOralOPVTrivaProductfa4849f7532d522134f4102063af1617"))
        
        // Test invalid cases
        assertFalse("Should reject null", IcvpValidation.validateIcvpProductId(null))
        assertFalse("Should reject empty", IcvpValidation.validateIcvpProductId(""))
        assertFalse("Should reject blank", IcvpValidation.validateIcvpProductId("   "))
        assertFalse("Should reject short IDs", IcvpValidation.validateIcvpProductId("short"))
        
        // Test format validation (fallback when PreQual database is not available)
        assertTrue("Should accept alphanumeric format", 
            IcvpValidation.validateIcvpProductId("SomeNewVaccineProduct123456789abcdef"))
        assertFalse("Should reject special characters", 
            IcvpValidation.validateIcvpProductId("Invalid-Product-ID@#$"))
    }
    
    @Test
    fun testProductIdCacheRefresh() {
        // Test cache refresh functionality
        IcvpValidation.refreshProductIdCache()
        
        // Should still validate based on format when source is not available
        assertTrue("Should accept valid format after cache refresh", 
            IcvpValidation.validateIcvpProductId("ValidProductId123456789"))
    }
}