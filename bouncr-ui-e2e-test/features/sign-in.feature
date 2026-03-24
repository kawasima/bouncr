Feature: Sign In

  Scenario: Initial password change on first login
    Given a user "e2e_init_user" exists with initial password "InitP@ss1"
    And I am on the sign-in page
    When I enter account "e2e_init_user" and password "InitP@ss1"
    And I click the sign-in button
    Then I should be on the change password page
    When I fill in "new_password" with "NewP@ss123"
    And I fill in "confirm_password" with "NewP@ss123"
    And I click the confirm button
    Then I should be on the sign-in page

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
