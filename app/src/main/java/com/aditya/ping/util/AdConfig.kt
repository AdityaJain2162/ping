package com.aditya.ping.util

import com.google.android.gms.ads.AdRequest

/**
 * AdMob configuration. These are Google's official **test** ad unit IDs — safe
 * for development and guaranteed not to generate real revenue.
 *
 * ⚠️ Replace with your own AdMob ad unit IDs before publishing to the Play
 * Store. Shipping with test IDs can lead to AdMob account suspension.
 *
 * Ping uses **banner ads only** — no interstitial, rewarded, or popup ads.
 * Banners appear at the bottom of every screen's scrollable content.
 */
object AdConfig {

    // Banner — Google's official Android test banner ad unit.
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    // Set to true in debug builds so all requests return test ads on registered devices.
    const val USE_TEST_DEVICE = true

    // Test device IDs — emulator + physical test devices.
    // Add your device's hashed ID (shown in logcat when an ad request fails) here.
    val TEST_DEVICE_IDS = listOf(
        AdRequest.DEVICE_ID_EMULATOR,
        "6618F766D6427DAD8C482D8D7E6B6EFB", // physical test device
    )
}
