package org.who.gdhcnvalidator.trust.pathcheck

import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.trust.TrustRegistryProvider

open class PCFTrustRegistryProvider : TrustRegistryProvider() {
    override fun create(): TrustRegistry {
        return PCFTrustRegistry()
    }
}