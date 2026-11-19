package org.who.gdhcnvalidator.trust

open abstract class TrustRegistryProvider {
    abstract fun create(): TrustRegistry
}