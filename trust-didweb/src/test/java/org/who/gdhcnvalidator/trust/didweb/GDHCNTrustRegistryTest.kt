package org.who.gdhcnvalidator.trust.didweb

import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.trust.didweb.DDCCTrustRegistryTest.Companion.registry

class GDHCNTrustRegistryTest {
    companion object {
        val registry = GDHCNTrustRegistry()

        @JvmStatic
        @BeforeClass
        fun setup(): Unit {
            registry.init(
                GDHCNTrustRegistry.PRODUCTION_REGISTRY,
                GDHCNTrustRegistry.ACCEPTANCE_REGISTRY,
                GDHCNTrustRegistry.DEV_STAGING_REGISTRY
            )
        }
    }

    @Test
    fun testDevWHO() {
        val t = registry.resolve(TrustRegistry.Framework.DCC, "xcl#UUuJcwmjoJM=")
        assertNotNull(t)
    }
}