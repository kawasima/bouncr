Feature: Sign In

  Scenario: Successful sign-in
    Given I am on the sign-in page
    When I enter account "admin" and the admin password
    And I click the sign-in button
    Then I should see the home page

  Scenario: Failed sign-in with wrong password
    Given I am on the sign-in page
    When I enter account "admin" and password "wrongpassword"
    And I click the sign-in button
    Then I should see an authentication error

  Scenario: Sign out
    Given I am signed in as "admin"
    When I click the sign-out button
    Then I should be on the sign-in page
