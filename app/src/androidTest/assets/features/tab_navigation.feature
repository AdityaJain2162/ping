Feature: Tab Navigation
  As a user
  I want to switch between tabs
  So that I can access different parts of the app

  Scenario: User navigates to the Calendar tab
    Given the app is on the home screen
    When I tap the "Calendar" tab
    Then I should be on the "Calendar" screen

  Scenario: User navigates to the Saved Places tab
    Given the app is on the home screen
    When I tap the "Saved" tab
    Then I should be on the "Saved Places" screen

  Scenario: User navigates to the Automations tab
    Given the app is on the home screen
    When I tap the "Automations" tab
    Then I should be on the "Automations" screen

  Scenario: User returns to Home from another tab
    Given the app is on the home screen
    When I tap the "Calendar" tab
    Then I should be on the "Calendar" screen
    When I tap the "Home" tab
    Then I should be on the "Your Reminders" screen
