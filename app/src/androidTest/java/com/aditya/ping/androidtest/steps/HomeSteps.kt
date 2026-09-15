package com.aditya.ping.androidtest.steps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class HomeSteps(
    private val composeRuleHolder: ComposeRuleHolder,
) {

    private val composeRule: ComposeTestRule
        get() = composeRuleHolder.composeRule

    @Given("the app is on the home screen")
    fun appIsOnHomeScreen() {
        composeRule.waitForIdle()
    }

    @When("I tap the add button")
    fun iTapTheAddButton() {
        composeRule.onNodeWithTag("fab").performClick()
        composeRule.waitForIdle()
    }

    @When("I enter {string} as the title")
    fun iEnterTitle(title: String) {
        composeRule.onNodeWithText("Title").performTextInput(title)
        composeRule.waitForIdle()
    }

    @When("I tap save")
    fun iTapSave() {
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
    }

    @When("I tap cancel")
    fun iTapCancel() {
        androidx.test.espresso.Espresso.pressBack()
        composeRule.waitForIdle()
    }

    @Then("I should see {string} in the reminder list")
    fun iShouldSeeReminder(title: String) {
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Then("I should see the empty state")
    fun iShouldSeeEmptyState() {
        composeRule.onNodeWithText("No reminders yet").assertIsDisplayed()
    }

    @Then("I should see the add reminder form")
    fun iShouldSeeAddForm() {
        composeRule.onNodeWithText("New Reminder").assertIsDisplayed()
    }

    @Then("I should see the edit reminder form")
    fun iShouldSeeEditForm() {
        composeRule.onNodeWithText("New Reminder").assertIsDisplayed()
    }

    @Then("I should not see {string}")
    fun iShouldNotSee(text: String) {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }
}
