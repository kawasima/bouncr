Feature: Invitation

  Scenario: Create an invitation
    Given I am signed in as "admin"
    And I am on the admin "invitations" page
    When I fill in "email" with "e2e_invite@example.com"
    And I click "Create"
    Then I should see "Invitation Created"
