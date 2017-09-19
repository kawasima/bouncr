package backendexample;


import enkan.system.EnkanSystem;
import enkan.util.BeanBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.unit8.bouncr.cert.CertificateProvider;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import static enkan.component.ComponentRelationship.component;

public class Certificate {
    private static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);
    private static final Date NOT_AFTER  = new Date(System.currentTimeMillis() + 86400000L * 365 * 100);

    public static X500PrivateCredential generateServerCertificate(KeyPair caKeyPair) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertIOException {
        X500Name issuerName = new X500Name("CN=bouncrca");
        X500Name subjectName = new X500Name("CN=bouncr");
        BigInteger serial = BigInteger.valueOf(2);
        long t1 = System.currentTimeMillis();
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048, SecureRandom.getInstance("NativePRNGNonBlocking"));
        KeyPair kp = rsa.generateKeyPair();
        System.out.println(System.currentTimeMillis() - t1);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, NOT_BEFORE, NOT_AFTER, subjectName, kp.getPublic());
        DERSequence subjectAlternativeNames = new DERSequence(new ASN1Encodable[] {
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.dNSName, "127.0.0.1")
        });
        builder.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);
        X509Certificate cert = signCertificate(builder, caKeyPair.getPrivate());

        return new X500PrivateCredential(cert, kp.getPrivate());
    }

    public static X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder, PrivateKey caPrivateKey) throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(caPrivateKey);
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(signer));
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048, SecureRandom.getInstance("NativePRNGNonBlocking"));
        KeyPair kp = rsa.generateKeyPair();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);

        X500Name issuerName = new X500Name("CN=bouncrca");
        X500Name subjectName = issuerName;
        BigInteger serial = BigInteger.ONE;
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, NOT_BEFORE, NOT_AFTER, subjectName, kp.getPublic());

        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));
        X509Certificate cacert = signCertificate(builder, kp.getPrivate());

        X500PrivateCredential serverCert = generateServerCertificate(kp);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        try(OutputStream out = new FileOutputStream("src/dev/resources/bouncr.jks")) {
            keyStore.setKeyEntry("bouncr", serverCert.getPrivateKey()
                    , "password".toCharArray(),
                    new java.security.cert.Certificate[]{serverCert.getCertificate() });
            keyStore.store(out, "password".toCharArray());
        }

        EnkanSystem system = EnkanSystem.of(
                "certificate", BeanBuilder.builder(new CertificateProvider())
                        .set(CertificateProvider::setCA, new X500PrivateCredential(cacert, kp.getPrivate()))
                        .build(),
                "flake", new Flake(),
                "config", new BouncrConfiguration()
        ).relationships(
                component("certificate").using("flake", "config")
        );
        system.start();
        CertificateProvider certificateProvider = (CertificateProvider) system.getComponent("certificate");
        X500PrivateCredential clientCredential = certificateProvider.generateClientCertificate(
                "CN=admin");
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);
        try(OutputStream out = new FileOutputStream("src/dev/resources/bouncr_clients.jks")) {
            trustStore.setCertificateEntry("admin", clientCredential.getCertificate());
            trustStore.setCertificateEntry("bouncrca", cacert);
            trustStore.store(out, "password".toCharArray());
        }

        KeyStore trustPkcs12 = KeyStore.getInstance("PKCS12");
        trustPkcs12.load(null, null);
        try(OutputStream out = new FileOutputStream("src/dev/resources/bouncr_clients.p12")) {
            trustPkcs12.setKeyEntry("admin", clientCredential.getPrivateKey(), "password".toCharArray()
                    , new java.security.cert.Certificate[]{ clientCredential.getCertificate() });
            trustPkcs12.store(out, "password".toCharArray());
        }

    }
}
