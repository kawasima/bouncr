package backendexample;


import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.Date;

public class Certificate {
    public static void main(String[] args) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(4096);
        KeyPair kp = rsa.generateKeyPair();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);

        byte[] pk = kp.getPublic().getEncoded();
        SubjectPublicKeyInfo bcPk = SubjectPublicKeyInfo.getInstance(pk);


        X509v1CertificateBuilder certGen = new X509v1CertificateBuilder(
                new X500Name("CN=CA Cert"),
                BigInteger.ONE,
                new Date(),
                cal.getTime(),
                new X500Name("CN=CA Cert"),
                bcPk
        );

        X509CertificateHolder certHolder = certGen
                .build(new JcaContentSignerBuilder("SHA1withRSA").build(kp.getPrivate()));

        BASE64Encoder encoder = new BASE64Encoder();

        System.out.println("CA CERT");
        System.out.println(X509Factory.BEGIN_CERT);
        encoder.encodeBuffer(certHolder.getEncoded(), System.out);
        System.out.println(X509Factory.END_CERT);
    }
}
