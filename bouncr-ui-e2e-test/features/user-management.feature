Feature: User Management

  Scenario: Create a new user
    Given I am signed in as "admin"
    And I am on the admin "users" page
    When I click "New"
    And I fill in "account" with "e2e_new_user"
    And I fill in "name" with "E2E New User"
    And I fill in "email" with "e2e_new@example.com"
    And I click "Save"
    Then I should see a success indication

  Scenario: Delete a user
    Given I am signed in as "admin"
    And a user "e2e_to_delete" exists
    And I am on the admin "users" page
    When I click on "e2e_to_delete" in the list
    And I click "Delete" and confirm
    Then I should return to the list view
