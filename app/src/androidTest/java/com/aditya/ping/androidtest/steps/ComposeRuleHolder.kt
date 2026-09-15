package com.aditya.ping.androidtest.steps

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.aditya.ping.MainActivity
import com.aditya.ping.data.PingDatabase
import io.cucumber.java.After
import io.cucumber.java.Before
import kotlinx.coroutines.CoroutineExceptionHandler
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.ServiceLoader
import java.util.concurrent.CountDownLatch

class ComposeRuleHolder {

    val composeRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<*>? = null
    private var ruleThread: Thread? = null
    private val scenarioLatch = CountDownLatch(1)
    private val setupLatch = CountDownLatch(1)

    @Before
    fun setUp() {
        Thread.currentThread().contextClassLoader = javaClass.classLoader
        fixCoroutineExceptionHandlers()

        // The AndroidComposeTestRule sets up the Compose test environment in
        // its Statement.evaluate() method, and tears it down after evaluate()
        // returns. We need the environment to stay alive for the whole scenario.
        // Solution: run evaluate() on a separate thread with a base Statement
        // that blocks until the @After hook releases it.
        val base = object : Statement() {
            override fun evaluate() {
                // Signal that setup is complete
                setupLatch.countDown()
                // Block until the scenario is done
                scenarioLatch.await()
            }
        }
        val description = Description.createTestDescription("Cucumber", "Scenario")

        ruleThread = Thread {
            try {
                composeRule.apply(base, description).evaluate()
            } catch (e: Throwable) {
                Log.e("PingCucumber", "Compose rule thread failed", e)
            }
        }.also { it.start() }

        // Wait for the Compose test environment to be set up
        setupLatch.await()

        // Clear the database so each scenario starts fresh
        clearDatabase()

        // Launch the MainActivity
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MainActivity::class.java,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        scenario?.close()
        scenario = ActivityScenario.launch<Activity>(intent)
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
        // Release the base statement so evaluate() can complete and tear down
        scenarioLatch.countDown()
        ruleThread?.join(5000)
        ruleThread = null
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
