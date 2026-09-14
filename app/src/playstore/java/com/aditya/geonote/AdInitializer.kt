package com.aditya.geonote

import android.content.Context
import com.google.android.gms.ads.MobileAds

/**
 * Play Store flavor: initializes AdMob on app launch.
 */
object AdInitializer {
    fun init(context: Context) {
        MobileAds.initialize(context) {}
    }
}
