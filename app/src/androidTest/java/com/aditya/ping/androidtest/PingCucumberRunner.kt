package com.aditya.ping.androidtest

import android.os.Bundle
import android.util.Log
import io.cucumber.android.runner.CucumberAndroidJUnitRunner
import java.util.Timer
import kotlin.concurrent.timerTask

class PingCucumberRunner : CucumberAndroidJUnitRunner() {

    override fun onStart() {
        // AndroidJUnitRunner.onStart() -> MonitoringInstr.onStart() resets the
        // thread's context classloader to the app classloader (main APK only).
        // ServiceLoader.load(FeatureParser.class) then can't find
        // GherkinMessagesFeatureParser which lives in the test APK.
        //
        // Fix: use a Timer to continuously restore the test classloader on the
        // instrumentation thread, so it's correct when buildRequest() runs.
        val testClassLoader = javaClass.classLoader!!
        val instrThread = Thread.currentThread()

        Log.i("PingCucumber", "onStart: fixing context classloader")
        instrThread.contextClassLoader = testClassLoader

        val timer = Timer("classloader-restorer", true)
        timer.scheduleAtFixedRate(timerTask {
            instrThread.contextClassLoader = testClassLoader
        }, 0, 1)

        try {
            super.onStart()
        } finally {
            timer.cancel()
        }
    }
}
