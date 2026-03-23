Feature: Group Management

  Scenario: Create a group
    Given I am signed in as "admin"
    And I am on the admin "groups" page
    When I click "New"
    And I fill in "name" with "e2e_new_group"
    And I fill in "description" with "E2E Test Group"
    And I click "Save"
    Then I should see a success indication

  Scenario: Add user to group
    Given I am signed in as "admin"
    And a group "e2e_member_group" exists
    And a user "e2e_member_user" exists
    And I am on the admin "groups" page
    When I click on "e2e_member_group" in the list
    And I search for user "e2e_member_user"
    And I click on the search result "e2e_member_user"
    Then I should see "e2e_member_user" in the group members

  Scenario: Delete a group
    Given I am signed in as "admin"
    And a group "e2e_del_group" exists
    And I am on the admin "groups" page
    When I click on "e2e_del_group" in the list
    And I click "Delete Group" and confirm
    Then I should return to the list view
