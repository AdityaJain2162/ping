package com.aditya.ping

import android.content.Context
import android.util.Log
import com.aditya.ping.util.AdConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Initializes AdMob on app launch.
 * Sets test device configuration once globally before any ad loads.
 */
object AdInitializer {
    private const val TAG = "AdInitializer"

    fun init(context: Context) {
        MobileAds.initialize(context) {}

        if (AdConfig.USE_TEST_DEVICE) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(AdConfig.TEST_DEVICE_IDS)
                    .build(),
            )
            Log.d(TAG, "Test device IDs configured: ${AdConfig.TEST_DEVICE_IDS}")
        }
    }
}
