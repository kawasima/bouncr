package net.unit8.bouncr.api.service;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.server.ServerProperty;
import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.WebAuthnCredential;

import java.nio.ByteBuffer;
import java.util.List;

public class WebAuthnService {
    private final BouncrConfiguration config;
    private final WebAuthnManager webAuthnManager;
    private final AttestedCredentialDataConverter attestedCredentialDataConverter;

    private static final List<PublicKeyCredentialParameters> PUB_KEY_CRED_PARAMS = List.of(
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
    );

    public WebAuthnService(BouncrConfiguration config) {
        if (config.getWebAuthnRpId() == null || config.getWebAuthnRpId().isBlank()) {
            throw new MisconfigurationException("bouncr.WEBAUTHN_RP_ID_REQUIRED");
        }
        if (config.getWebAuthnOrigin() == null || config.getWebAuthnOrigin().isBlank()) {
            throw new MisconfigurationException("bouncr.WEBAUTHN_ORIGIN_REQUIRED");
        }
        this.config = config;
        this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
        this.attestedCredentialDataConverter = new AttestedCredentialDataConverter(new ObjectConverter());
    }

    public byte[] generateChallenge() {
        byte[] challenge = new byte[32];
        config.getSecureRandom().nextBytes(challenge);
        return challenge;
    }

    public static byte[] userIdToHandle(Long userId) {
        return ByteBuffer.allocate(8).putLong(userId).array();
    }

    public RegistrationData verifyRegistration(String registrationResponseJSON, byte[] challengeBytes) {
        RegistrationData registrationData = webAuthnManager.parseRegistrationResponseJSON(registrationResponseJSON);

        Origin origin = new Origin(config.getWebAuthnOrigin());
        Challenge challenge = new DefaultChallenge(challengeBytes);
        ServerProperty serverProperty = ServerProperty.builder()
                .origin(origin)
                .rpId(config.getWebAuthnRpId())
                .challenge(challenge)
                .build();

        RegistrationParameters params = new RegistrationParameters(
                serverProperty,
                PUB_KEY_CRED_PARAMS,
                false,  // userVerificationRequired
                true    // userPresenceRequired
        );

        webAuthnManager.verify(registrationData, params);
        return registrationData;
    }

    public void verifyAuthenticationData(AuthenticationData authenticationData,
                                        byte[] challengeBytes,
                                        WebAuthnCredential storedCredential) {
        Origin origin = new Origin(config.getWebAuthnOrigin());
        Challenge challenge = new DefaultChallenge(challengeBytes);
        ServerProperty serverProperty = ServerProperty.builder()
                .origin(origin)
                .rpId(config.getWebAuthnRpId())
                .challenge(challenge)
                .build();

        AttestedCredentialData attestedCredentialData = attestedCredentialDataConverter.convert(storedCredential.publicKey());

        CredentialRecord credentialRecord = new CredentialRecordImpl(
                null,  // attestationStatement
                null,  // uvInitialized
                null,  // backupEligible
                null,  // backupState
                storedCredential.signCount(),
                attestedCredentialData,
                null,  // authenticatorExtensions
                null,  // clientData
                null,  // clientExtensions
                null   // transports
        );

        AuthenticationParameters params = new AuthenticationParameters(
                serverProperty,
                credentialRecord,
                null,   // allowCredentials
                false,  // userVerificationRequired
                true    // userPresenceRequired
        );

        webAuthnManager.verify(authenticationData, params);
    }

    public byte[] serializeAttestedCredentialData(RegistrationData registrationData) {
        AttestedCredentialData attestedCredentialData =
                registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        return attestedCredentialDataConverter.convert(attestedCredentialData);
    }

    public List<PublicKeyCredentialParameters> getPubKeyCredParams() {
        return PUB_KEY_CRED_PARAMS;
    }

    public WebAuthnManager getWebAuthnManager() {
        return webAuthnManager;
    }
}
