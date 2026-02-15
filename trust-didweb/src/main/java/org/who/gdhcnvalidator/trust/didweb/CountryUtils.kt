package org.who.gdhcnvalidator.trust.didweb

import java.util.*
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.text.contains
import kotlin.text.lowercase
import kotlin.to

class CountryUtils {
    /*
    This is a Map of 'virtual' countries that do not exist and therefore do not have
    a regular iso-3166 alpha2/alpha3 mapping
     */
    val virtualCountryMap: MutableMap<String,String> = mutableMapOf(
        "xa" to "xxa",
        "xb" to "xxb",
        "xc" to "xxc",
        "xd" to "xxd",
        "xe" to "xxe",
        "xf" to "xxf",
        "xg" to "xxg",
        "xh" to "xxh",
        "xi" to "xxi",
        "xj" to "xxj",
        "xk" to "xxk",
        "xl" to "xxl",
        "xm" to "xxm",
        "xn" to "xxn",
        "xo" to "xxo",
        "xp" to "xxp",
        "xu" to "xxu",
        "io" to "iot" //change this to 'iom' which will then override the correct iso3166 mapping io->iot
    )
    fun getAlpha3Country(country: String) : String {
        try {
            virtualCountryMap.entries.filter { it.key.contains(country) }
                .map {
                return it.value
                }
            return Locale("en", country).isO3Country.lowercase(Locale.getDefault())
        } catch (e: Exception) {
            println("Exception encountered while getting alpha3 country $country: ${e.message}")
            return ""
        }
    }
}