Feature: Add Reminder
  As a user
  I want to add a new reminder
  So that I never forget something important

  Scenario: User adds a simple time reminder
    Given the app is on the home screen
    When I tap the add button
    Then I should see the add reminder form
    When I enter "Buy groceries" as the title
    And I tap save
    Then I should see "Buy groceries" in the reminder list

  Scenario: User cancels adding a reminder
    Given the app is on the home screen
    When I tap the add button
    Then I should see the add reminder form
    When I tap cancel
    Then I should not see "New Reminder"
