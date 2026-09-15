package com.aditya.ping

import android.content.Context
import com.aditya.ping.util.AdConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Play Store flavor: initializes AdMob on app launch.
 * Sets test device configuration once globally before any ad loads.
 */
object AdInitializer {
    fun init(context: Context) {
        MobileAds.initialize(context) {}

        if (AdConfig.USE_TEST_DEVICE) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build(),
            )
        }
    }
}
