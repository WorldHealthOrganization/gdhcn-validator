package org.who.gdhcnvalidator.trust

class CompoundRegistry(
    val registries: List<TrustRegistry>,
) : TrustRegistry {
    override fun scopeNames(): List<TrustRegistry.ILoadedRegistry> {
        return registries.map {
            it.scopeNames()
        }.flatten()
    }

    override fun init() {
        registries.forEach {
            it.init()
        }
    }

    override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {
        registries.forEach {
            it.init(*customRegistries)
        }
    }

    override fun resolve(
        framework: TrustRegistry.Framework,
        kid: String,
    ): TrustRegistry.TrustedEntity? =
        registries.firstNotNullOfOrNull {
            it.resolve(framework, kid)
        }
}
