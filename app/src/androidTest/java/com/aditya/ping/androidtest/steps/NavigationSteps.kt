package com.aditya.ping.androidtest.steps

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class NavigationSteps(
    private val composeRuleHolder: ComposeRuleHolder,
) {

    private val composeRule: ComposeTestRule
        get() = composeRuleHolder.composeRule

    private val tabTags = mapOf(
        "Home" to "tab_home",
        "Saved" to "tab_saved_places",
        "Calendar" to "tab_calendar",
        "Automations" to "tab_automations",
    )

    @When("I tap the {string} tab")
    fun iTapTab(tabName: String) {
        val tag = tabTags[tabName] ?: throw IllegalArgumentException("Unknown tab: $tabName")
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    @Then("I should be on the {string} screen")
    fun iShouldBeOnScreen(screenTitle: String) {
        composeRule.waitUntil(5000) {
            try {
                if (screenTitle == "Your Reminders") {
                    // FAB is only visible on the Home tab
                    composeRule.onNodeWithTag("fab").assertIsDisplayed()
                } else {
                    composeRule.onNodeWithTag("topBarTitle").assertIsDisplayed()
                }
                true
            } catch (e: Throwable) {
                false
            }
        }
        if (screenTitle != "Your Reminders") {
            composeRule.onNodeWithTag("topBarTitle").assertTextEquals(screenTitle)
        }
    }

    @Then("I should see the calendar grid")
    fun iShouldSeeCalendarGrid() {
        composeRule.onNodeWithTag("topBarTitle").assertIsDisplayed()
    }
}
