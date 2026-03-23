Feature: Admin Access Control

  Scenario: Admin sees all menu items
    Given I am signed in as "admin"
    Then I should see admin menu items:
      | Users | Groups | Applications | Roles | Permissions | OIDC Applications | OIDC Providers | Invitations | Audit |

  Scenario: Read-only user sees menu but not New button
    Given I am signed in as "e2e_readonly"
    When I navigate to "/admin/users"
    Then I should not see the "New" button
    And form inputs should be disabled

  Scenario: No-admin user sees no admin menu
    Given I am signed in as "e2e_no_admin"
    Then I should not see the admin menu

  Scenario: No-admin user gets 403 on direct URL
    Given I am signed in as "e2e_no_admin"
    When I navigate to "/admin/users"
    Then I should see "403"

  Scenario: Group-only user sees only Groups menu
    Given I am signed in as "e2e_group_only"
    Then I should see admin menu items:
      | Groups |
    And I should not see admin menu item "Users"
