package net.unit8.bouncr.component;

import enkan.system.EnkanSystem;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtHeader;
import net.unit8.bouncr.sign.JwtClaim;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class JsonWebTokenTest {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    @Test
    public void test() throws IOException, NoSuchAlgorithmException {
        EnkanSystem system = EnkanSystem.of(
                "jsonWebToken", new JsonWebToken(),
                "config", new BouncrConfiguration()
        ).relationships(
                component("jsonWebToken").using("config")
        );
        system.start();
        JsonWebToken jsonWebToken = (JsonWebToken)system.getComponent("jsonWebToken");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair kp = keyGen.generateKeyPair();

        JwtHeader header = builder(new JwtHeader())
                .set(JwtHeader::setAlg, "RS256")
                .set(JwtHeader::setKid, "keyid")
                .build();
        JwtClaim payload = builder(new JwtClaim())
                .set(JwtClaim::setSub, "kawasima")
                .set(JwtClaim::setIss, "https://localhost:3002")
                .build();
        String sign = jsonWebToken.sign(payload, header, kp.getPrivate());
        System.out.println(sign);
    }
}
