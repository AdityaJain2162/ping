package com.aditya.geonote.util

/**
 * AdMob configuration. These are Google's official **test** ad unit IDs — safe
 * for development and guaranteed not to generate real revenue.
 *
 * ⚠️ Replace with your own AdMob ad unit IDs before publishing to the Play
 * Store. Shipping with test IDs can lead to AdMob account suspension.
 */
object AdConfig {

    // Banner — Google's official Android test banner ad unit.
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    // Interstitial — test unit (reserved for future use).
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Rewarded — test unit (reserved for future use).
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Set to true in debug builds so all requests return test ads on registered devices.
    const val USE_TEST_DEVICE = true
}
