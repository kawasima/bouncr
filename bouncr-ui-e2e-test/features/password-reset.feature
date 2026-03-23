Feature: Password Reset

  Scenario: Request password reset shows success message
    Given I am on the sign-in page
    When I click "Forgot password?"
    And I fill in "account" with "admin"
    And I click "Send Reset Code"
    Then I should see the reset code sent message
