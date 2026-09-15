package com.aditya.ping.androidtest.steps

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.aditya.ping.MainActivity
import com.aditya.ping.data.PingDatabase
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.junit.WithJunitRule
import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.ServiceLoader

class ComposeRuleHolder {

    @field:WithJunitRule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        Thread.currentThread().contextClassLoader = javaClass.classLoader
        fixCoroutineExceptionHandlers()
        clearDatabase()
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        // Activity is managed by the rule
    }

    private fun fixCoroutineExceptionHandlers() {
        try {
            val testCl = javaClass.classLoader!!
            val testHandlers = ServiceLoader.load(CoroutineExceptionHandler::class.java, testCl).toList()

            val implKtClass = Class.forName("kotlinx.coroutines.internal.CoroutineExceptionHandlerImplKt")
            val field = implKtClass.getDeclaredField("platformExceptionHandlers")
            field.isAccessible = true

            val currentValue = field.get(null) as? Collection<*>
            val combined = mutableListOf<Any>()
            @Suppress("UNCHECKED_CAST")
            currentValue?.let { combined.addAll(it as Collection<Any>) }
            combined.addAll(testHandlers)

            field.set(null, combined)
        } catch (e: Exception) {
            Log.e("PingCucumber", "fixCoroutineExceptionHandlers failed", e)
        }
    }

    private fun clearDatabase() {
        try {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val db = PingDatabase.get(context)
            db.openHelper.writableDatabase.execSQL("DELETE FROM reminders")
            db.openHelper.writableDatabase.execSQL("DELETE FROM saved_places")
            db.openHelper.writableDatabase.execSQL("DELETE FROM reminder_lists")
            db.openHelper.writableDatabase.execSQL("DELETE FROM automations")
        } catch (e: Exception) {
            Log.e("PingCucumber", "clearDatabase failed", e)
        }
    }
}
