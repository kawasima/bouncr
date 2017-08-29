package backendexample;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class Certificate {
    private static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);
    private static final Date NOT_AFTER  = new Date(System.currentTimeMillis() + 86400000L * 365 * 100);

    @Data
    @AllArgsConstructor
    public static class CertificateAndPrivKey {
        private X509Certificate certificate;
        private PrivateKey privateKey;
    }

    public static CertificateAndPrivKey generateServerCertificate(KeyPair caKeyPair) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertIOException {
        X500Name issuerName = new X500Name("CN=bouncrca");
        X500Name subjectName = new X500Name("CN=bouncr");
        BigInteger serial = BigInteger.valueOf(2);
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(4096);
        KeyPair kp = rsa.generateKeyPair();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, NOT_BEFORE, NOT_AFTER, subjectName, kp.getPublic());
        DERSequence subjectAlternativeNames = new DERSequence(new ASN1Encodable[] {
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.dNSName, "127.0.0.1")
        });
        builder.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);
        X509Certificate cert = signCertificate(builder, caKeyPair.getPrivate());

        return new CertificateAndPrivKey(cert, kp.getPrivate());
    }

    public static CertificateAndPrivKey generateClientCertificate(KeyPair caKeyPair) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertIOException, InvalidKeyException {
        X500Name issuerName = new X500Name("CN=bouncrca");
        X500Name subjectName = new X500Name("CN=admin");
        BigInteger serial = BigInteger.valueOf(3);
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(4096);
        KeyPair kp = rsa.generateKeyPair();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, NOT_BEFORE, NOT_AFTER, subjectName, kp.getPublic());
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.dataEncipherment | KeyUsage.keyAgreement));
        builder.addExtension(Extension.authorityKeyIdentifier, true, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(caKeyPair.getPublic()));
        DERSequence subjectAlternativeNames = new DERSequence(new ASN1Encodable[] {
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.dNSName, "127.0.0.1")
        });
        builder.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);

        X509Certificate cert = signCertificate(builder, caKeyPair.getPrivate());

        return new CertificateAndPrivKey(cert, kp.getPrivate());
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
        rsa.initialize(4096);
        KeyPair kp = rsa.generateKeyPair();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);

        X500Name issuerName = new X500Name("CN=bouncrca");
        X500Name subjectName = issuerName;
        BigInteger serial = BigInteger.ONE;
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, NOT_BEFORE, NOT_AFTER, subjectName, kp.getPublic());

        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));
        X509Certificate cert = signCertificate(builder, kp.getPrivate());

        CertificateAndPrivKey serverCert = generateServerCertificate(kp);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        try(OutputStream out = new FileOutputStream("src/dev/resources/bouncr.jks")) {
            keyStore.setKeyEntry("bouncr", serverCert.getPrivateKey()
                    , "password".toCharArray(),
                    new java.security.cert.Certificate[]{serverCert.getCertificate() });
            keyStore.store(out, "password".toCharArray());
        }

        CertificateAndPrivKey clientCert = generateClientCertificate(kp);
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);
        try(OutputStream out = new FileOutputStream("src/dev/resources/bouncr_clients.jks")) {
            trustStore.setKeyEntry("kawasima", clientCert.getPrivateKey(), "password".toCharArray()
                    , new java.security.cert.Certificate[]{ clientCert.getCertificate() });
            trustStore.setKeyEntry("bouncrca", kp.getPrivate(), "password".toCharArray(),
                    new java.security.cert.Certificate[]{ cert });
            trustStore.store(out, "password".toCharArray());
        }

        KeyStore trustPkcs12 = KeyStore.getInstance("PKCS12");
        trustPkcs12.load(null, null);
        try(OutputStream out = new FileOutputStream("src/dev/resources/bouncr_clients.p12")) {
            trustPkcs12.setKeyEntry("kawasima", clientCert.getPrivateKey(), "password".toCharArray()
                    , new java.security.cert.Certificate[]{ clientCert.getCertificate() });
            trustPkcs12.store(out, "password".toCharArray());
        }

    }
}
