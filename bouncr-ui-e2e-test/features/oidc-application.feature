Feature: OIDC Application

  Scenario: Create client_credentials application
    Given no OIDC application "e2e_cli_app" exists
    And I am signed in as "admin"
    And I am on the admin "oidc_applications" page
    When I click "New"
    And I fill in "name" with "e2e_cli_app"
    And I check "Client Credentials" grant type
    And I uncheck "Authorization Code" grant type
    And I uncheck "Refresh Token" grant type
    And I click "Save"
    Then I should see client credentials
    And the client ID should not be empty
    And the client secret should not be empty

  Scenario: Regenerate secret
    Given I am signed in as "admin"
    And an OIDC application "e2e_regen_app" exists
    And I am on the admin "oidc_applications" page
    When I click on "e2e_regen_app" in the list
    And I click "Regenerate Secret"
    Then I should see a new client secret
