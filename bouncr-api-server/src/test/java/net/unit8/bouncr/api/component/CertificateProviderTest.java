package net.unit8.bouncr.api.component;

import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.config.CertConfiguration;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500PrivateCredential;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateProviderTest {
    @Test
    void signCertificate_worksWithDefaultJdkProvider() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair caKeyPair = keyGen.generateKeyPair();
        KeyPair leafKeyPair = keyGen.generateKeyPair();

        X500Name caName = new X500Name("CN=TestCA");
        Date notBefore = new Date(System.currentTimeMillis() - 60_000);
        Date notAfter = new Date(System.currentTimeMillis() + 60_000);

        X509v3CertificateBuilder caBuilder = new JcaX509v3CertificateBuilder(
                caName, BigInteger.ONE, notBefore, notAfter, caName, caKeyPair.getPublic());
        ContentSigner caSigner = new JcaContentSignerBuilder("SHA256withRSA").build(caKeyPair.getPrivate());
        X509Certificate caCert = new JcaX509CertificateConverter().getCertificate(caBuilder.build(caSigner));

        CertificateProvider provider = new CertificateProvider();
        provider.setCA(new X500PrivateCredential(caCert, caKeyPair.getPrivate()));

        BouncrConfiguration config = new BouncrConfiguration();
        CertConfiguration certConfig = new CertConfiguration();
        config.setCertConfiguration(certConfig);
        config.setSecureRandom(new SecureRandom());
        provider.setConfigForTest(config);

        X500Name leafName = new X500Name("CN=Leaf");
        X509v3CertificateBuilder leafBuilder = new JcaX509v3CertificateBuilder(
                caName, BigInteger.TWO, notBefore, notAfter, leafName, leafKeyPair.getPublic());
        X509Certificate leafCert = provider.signCertificate(leafBuilder);

        assertThat(leafCert.getSubjectX500Principal().getName()).contains("CN=Leaf");
        leafCert.verify(caKeyPair.getPublic());
    }
}
