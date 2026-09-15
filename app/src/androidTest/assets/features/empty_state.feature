Feature: Empty State
  As a new user
  I want to see a helpful empty state
  So that I know how to get started

  Scenario: New user sees the empty state
    Given the app is on the home screen
    Then I should see the empty state

  Scenario: Empty state has a call to action
    Given the app is on the home screen
    Then I should see the empty state
    When I tap the add button
    Then I should see the add reminder form
