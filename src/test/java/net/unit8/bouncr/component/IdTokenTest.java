package net.unit8.bouncr.component;

import enkan.system.EnkanSystem;
import net.unit8.bouncr.sign.IdToken;
import net.unit8.bouncr.sign.IdTokenHeader;
import net.unit8.bouncr.sign.IdTokenPayload;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class IdTokenTest {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    @Test
    public void test() throws IOException, NoSuchAlgorithmException {
        EnkanSystem system = EnkanSystem.of(
                "idToken", new IdToken(),
                "config", new BouncrConfiguration()
        ).relationships(
                component("idToken").using("config")
        );
        system.start();
        IdToken idToken = (IdToken)system.getComponent("idToken");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair kp = keyGen.generateKeyPair();

        IdTokenHeader header = builder(new IdTokenHeader())
                .set(IdTokenHeader::setAlg, "RS256")
                .set(IdTokenHeader::setKid, "keyid")
                .build();
        IdTokenPayload payload = builder(new IdTokenPayload())
                .set(IdTokenPayload::setSub, "kawasima")
                .set(IdTokenPayload::setIss, "https://localhost:3002")
                .build();
        String sign = idToken.sign(payload, header, kp.getPrivate());
        System.out.println(sign);
    }
}
