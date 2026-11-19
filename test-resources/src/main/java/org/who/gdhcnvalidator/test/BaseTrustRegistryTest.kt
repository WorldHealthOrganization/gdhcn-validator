package org.who.gdhcnvalidator.test

import org.who.gdhcnvalidator.trust.CompoundRegistry
import org.who.gdhcnvalidator.trust.TrustRegistryFactory
import org.who.gdhcnvalidator.trust.pathcheck.PCFTrustRegistry
import java.io.InputStream
import kotlin.system.measureTimeMillis

open class BaseTrustRegistryTest {
    companion object {
        var registry = CompoundRegistry(TrustRegistryFactory.getTrustRegistries()).apply {
            val elapsed = measureTimeMillis {
                init()
            }
            println("TIME: Registry Loaded in $elapsed")
        }
    }

    fun inputStream(assetName: String): InputStream? {
        return javaClass.classLoader?.getResourceAsStream(assetName)
    }

    fun open(assetName: String): String {
        return inputStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }
}