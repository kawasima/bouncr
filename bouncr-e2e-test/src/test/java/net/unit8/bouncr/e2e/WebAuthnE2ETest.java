package net.unit8.bouncr.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAssertionResponse;
import com.webauthn4j.data.AuthenticatorAttestationResponse;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.ResidentKeyRequirement;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.test.EmulatorUtil;
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor;
import com.webauthn4j.test.client.ClientPlatform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e-full")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebAuthnE2ETest extends E2ETestBase {

    private APIRequestContext userApi;
    private ClientPlatform clientPlatform;

    @BeforeAll
    void setup() throws Exception {
        // Create a regular user with my:update permission
        userApi = authenticatedContext(1L, "admin",
                List.of("my:read", "my:update", "my:delete"));
        clientPlatform = new ClientPlatform(
                new Origin("http://localhost:13005"),
                new WebAuthnAuthenticatorAdaptor(EmulatorUtil.NONE_ATTESTATION_AUTHENTICATOR));
    }

    @AfterAll
    void cleanup() {
        if (userApi != null) userApi.dispose();
    }

    @Test
    @Order(1)
    void registerOptionsReturnsChallenge() throws Exception {
        APIResponse response = postJson(userApi, "/bouncr/api/my/webauthn/register/options", Map.of());
        assertThat(response.status()).isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> options = JSON.readValue(response.body(), Map.class);
        assertThat(options).containsKey("challenge");
        assertThat(options).containsKey("rp");
        assertThat(options).containsKey("user");
        assertThat(options).containsKey("pubKeyCredParams");

        // Verify Set-Cookie header with WEBAUTHN_SESSION_ID
        String setCookie = response.headers().get("set-cookie");
        assertThat(setCookie).contains("WEBAUTHN_SESSION_ID");
    }

    @Test
    @Order(2)
    void fullRegistrationAndAuthenticationFlow() throws Exception {
        // Step 1: Get registration options
        APIResponse regOptionsResponse = postJson(userApi, "/bouncr/api/my/webauthn/register/options", Map.of());
        assertThat(regOptionsResponse.status()).isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> regOptions = JSON.readValue(regOptionsResponse.body(), Map.class);
        String challengeB64 = (String) regOptions.get("challenge");
        byte[] challengeBytes = Base64.getUrlDecoder().decode(challengeB64);
        String sessionCookie = extractCookieValue(regOptionsResponse.headers().get("set-cookie"), "WEBAUTHN_SESSION_ID");

        // Step 2: Use ClientPlatform emulator to create credential
        @SuppressWarnings("unchecked")
        Map<String, Object> rpMap = (Map<String, Object>) regOptions.get("rp");
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) regOptions.get("user");

        PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(
                (String) rpMap.get("id"), (String) rpMap.get("name"));
        byte[] userId = Base64.getUrlDecoder().decode((String) userMap.get("id"));
        PublicKeyCredentialUserEntity user = new PublicKeyCredentialUserEntity(
                userId, (String) userMap.get("name"), (String) userMap.get("displayName"));

        AuthenticatorSelectionCriteria authenticatorSelection = new AuthenticatorSelectionCriteria(
                null, ResidentKeyRequirement.PREFERRED, UserVerificationRequirement.PREFERRED);

        PublicKeyCredentialCreationOptions creationOptions = new PublicKeyCredentialCreationOptions(
                rp, user,
                new DefaultChallenge(challengeBytes),
                List.of(new PublicKeyCredentialParameters(
                        PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
                null, null, authenticatorSelection, AttestationConveyancePreference.NONE, null);

        PublicKeyCredential<AuthenticatorAttestationResponse, ?> credential = clientPlatform.create(creationOptions);
        AuthenticatorAttestationResponse attestationResponse = credential.getResponse();

        // Serialize to the JSON format the server expects
        String registrationResponseJSON = buildRegistrationResponseJSON(credential, attestationResponse);

        // Step 3: Register the credential
        APIResponse regResponse = userApi.post("/bouncr/api/my/webauthn/register",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setHeader("Cookie", "WEBAUTHN_SESSION_ID=" + sessionCookie)
                        .setData(JSON.writeValueAsString(Map.of(
                                "registration_response_json", registrationResponseJSON,
                                "credential_name", "Test Passkey"))));
        assertThat(regResponse.status()).isEqualTo(201);

        // Step 4: Verify credential appears in list
        APIResponse listResponse = userApi.get("/bouncr/api/my/webauthn/credentials");
        assertThat(listResponse.status()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> creds = JSON.readValue(listResponse.body(), List.class);
        assertThat(creds).isNotEmpty();
        assertThat(creds.get(0).get("credential_name")).isEqualTo("Test Passkey");

        // Step 5: Get authentication options
        APIResponse authOptionsResponse = postJson(api, "/bouncr/api/sign_in/webauthn/options",
                Map.of("account", "admin"));
        assertThat(authOptionsResponse.status()).isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> authOptions = JSON.readValue(authOptionsResponse.body(), Map.class);
        String authChallenge = (String) authOptions.get("challenge");
        byte[] authChallengeBytes = Base64.getUrlDecoder().decode(authChallenge);
        String authSessionCookie = extractCookieValue(
                authOptionsResponse.headers().get("set-cookie"), "WEBAUTHN_SESSION_ID");

        // Step 6: Use ClientPlatform emulator to get assertion
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allowCredentials = (List<Map<String, Object>>) authOptions.get("allowCredentials");
        List<PublicKeyCredentialDescriptor> allowDescriptors = allowCredentials.stream()
                .map(ac -> new PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PUBLIC_KEY,
                        Base64.getUrlDecoder().decode((String) ac.get("id")),
                        null))
                .toList();

        PublicKeyCredentialRequestOptions requestOptions = new PublicKeyCredentialRequestOptions(
                new DefaultChallenge(authChallengeBytes),
                0L,
                (String) authOptions.get("rpId"),
                allowDescriptors,
                UserVerificationRequirement.PREFERRED,
                null);

        PublicKeyCredential<AuthenticatorAssertionResponse, ?> assertion = clientPlatform.get(requestOptions);
        AuthenticatorAssertionResponse assertionResponse = assertion.getResponse();

        String authenticationResponseJSON = buildAuthenticationResponseJSON(assertion, assertionResponse);

        // Step 7: Sign in with WebAuthn
        APIResponse signInResponse = api.post("/bouncr/api/sign_in/webauthn",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setHeader("Cookie", "WEBAUTHN_SESSION_ID=" + authSessionCookie)
                        .setData(JSON.writeValueAsString(Map.of(
                                "authentication_response_json", authenticationResponseJSON))));
        assertThat(signInResponse.status())
                .as("Sign-in response: %s", new String(signInResponse.body()))
                .isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> session = JSON.readValue(signInResponse.body(), Map.class);
        assertThat(session).containsKey("token");
    }

    @Test
    @Order(3)
    void credentialListAndDelete() throws Exception {
        APIResponse listResponse = userApi.get("/bouncr/api/my/webauthn/credentials");
        assertThat(listResponse.status()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> creds = JSON.readValue(listResponse.body(), List.class);

        if (!creds.isEmpty()) {
            int id = ((Number) creds.get(0).get("id")).intValue();
            APIResponse deleteResponse = userApi.delete("/bouncr/api/my/webauthn/credentials?id=" + id);
            assertThat(deleteResponse.status()).isBetween(200, 204);

            APIResponse listAfter = userApi.get("/bouncr/api/my/webauthn/credentials");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> credsAfter = JSON.readValue(listAfter.body(), List.class);
            assertThat(credsAfter).hasSize(creds.size() - 1);
        }
    }

    @Test
    @Order(4)
    void signInOptionsWithoutAccount() throws Exception {
        // Discoverable credential flow: no account specified
        APIResponse response = postJson(api, "/bouncr/api/sign_in/webauthn/options", Map.of());
        assertThat(response.status()).isEqualTo(201);

        @SuppressWarnings("unchecked")
        Map<String, Object> options = JSON.readValue(response.body(), Map.class);
        assertThat(options).containsKey("challenge");
        assertThat(options).containsKey("rpId");
        // allowCredentials should be empty for discoverable flow
        @SuppressWarnings("unchecked")
        List<?> allowCreds = (List<?>) options.get("allowCredentials");
        assertThat(allowCreds).isEmpty();
    }

    private String buildRegistrationResponseJSON(
            PublicKeyCredential<AuthenticatorAttestationResponse, ?> credential,
            AuthenticatorAttestationResponse response) throws Exception {
        Base64.Encoder b64url = Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", b64url.encodeToString(credential.getRawId()));
        json.put("rawId", b64url.encodeToString(credential.getRawId()));
        json.put("type", "public-key");
        json.put("response", Map.of(
                "attestationObject", b64url.encodeToString(response.getAttestationObject()),
                "clientDataJSON", b64url.encodeToString(response.getClientDataJSON())));
        json.put("clientExtensionResults", Map.of());
        return JSON.writeValueAsString(json);
    }

    private String buildAuthenticationResponseJSON(
            PublicKeyCredential<AuthenticatorAssertionResponse, ?> credential,
            AuthenticatorAssertionResponse response) throws Exception {
        Base64.Encoder b64url = Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", b64url.encodeToString(credential.getRawId()));
        json.put("rawId", b64url.encodeToString(credential.getRawId()));
        json.put("type", "public-key");
        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("authenticatorData", b64url.encodeToString(response.getAuthenticatorData()));
        responseMap.put("clientDataJSON", b64url.encodeToString(response.getClientDataJSON()));
        responseMap.put("signature", b64url.encodeToString(response.getSignature()));
        if (response.getUserHandle() != null) {
            responseMap.put("userHandle", b64url.encodeToString(response.getUserHandle()));
        }
        json.put("response", responseMap);
        json.put("clientExtensionResults", Map.of());
        return JSON.writeValueAsString(json);
    }

    private String extractCookieValue(String setCookieHeader, String name) {
        if (setCookieHeader == null) return null;
        for (String part : setCookieHeader.split(";")) {
            String trimmed = part.strip();
            if (trimmed.startsWith(name + "=")) {
                return trimmed.substring(name.length() + 1);
            }
        }
        return null;
    }
}
