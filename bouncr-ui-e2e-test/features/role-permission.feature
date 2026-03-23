Feature: Role and Permission Management

  Scenario: Create a role
    Given I am signed in as "admin"
    And I am on the admin "roles" page
    When I click "New"
    And I fill in "name" with "e2e_new_role"
    And I fill in "description" with "E2E Test Role"
    And I click "Save"
    Then I should see a success indication

  Scenario: Assign permissions to a role
    Given I am signed in as "admin"
    And a role "e2e_perm_role" exists
    And I am on the admin "roles" page
    When I click on "e2e_perm_role" in the list
    And I check permission "user:read"
    And I click "Save Permissions"
    Then the permission "user:read" should be checked
