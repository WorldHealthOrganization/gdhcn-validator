package org.who.gdhcnvalidator.trust.didweb

import org.who.gdhcnvalidator.trust.TrustRegistry
import org.who.gdhcnvalidator.trust.TrustRegistryProvider

open class GDHCNTrustRegistryProvider : TrustRegistryProvider() {
    override fun create(): TrustRegistry = GDHCNTrustRegistry()
}
