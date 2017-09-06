package net.unit8.bouncr.cert;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500PrivateCredential;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;

public class X509CertificateUtils {
    public static X500PrivateCredential generateClientCertificate(X500PrivateCredential cacert,
                                                                  String clientDn,
                                                                  long serial,
                                                                  Duration expiry
                                                                  ) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertIOException, InvalidKeyException {
        X500Name issuerName = new X500Name(cacert.getCertificate().getSubjectX500Principal().getName());
        X500Name subjectName = new X500Name(clientDn);

        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(4096);
        KeyPair kp = rsa.generateKeyPair();

        Date notBefore = new Date(System.currentTimeMillis() - 86400000L);
        Date notAfter  = new Date(System.currentTimeMillis() + expiry.getSeconds() * 1000);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName,
                BigInteger.valueOf(serial), notBefore, notAfter, subjectName, kp.getPublic());
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.dataEncipherment | KeyUsage.keyAgreement));
        builder.addExtension(Extension.authorityKeyIdentifier, true, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(cacert.getCertificate().getPublicKey()));
        DERSequence subjectAlternativeNames = new DERSequence(new ASN1Encodable[] {
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.dNSName, "127.0.0.1")
        });
        builder.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);

        // Sign by CA
        X509Certificate cert = signCertificate(builder, cacert.getPrivateKey());
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


}
