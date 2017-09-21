package net.unit8.bouncr.cert;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.MisconfigurationException;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import net.unit8.bouncr.util.KeyUtils;
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
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;

public class CertificateProvider extends SystemComponent {
    private BouncrConfiguration config;
    private Flake flake;

    private X500PrivateCredential ca;
    private Duration expiry;


    public X500PrivateCredential generateClientCertificate(String clientDn) {
        X500Name issuerName = new X500Name(ca.getCertificate().getSubjectX500Principal().getName());
        X500Name subjectName = new X500Name(clientDn);

        KeyPair kp = KeyUtils.generate(config.getCertConfiguration().getKeyLength(), config.getSecureRandom());

        Date notBefore = new Date(System.currentTimeMillis() - 86400000L);
        Date notAfter  = new Date(System.currentTimeMillis() + expiry.getSeconds() * 1000);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName,
                flake.generateId(), notBefore, notAfter, subjectName, kp.getPublic());
        try {
            builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(
                    KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.dataEncipherment | KeyUsage.keyAgreement));
            builder.addExtension(Extension.authorityKeyIdentifier, true, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(ca.getCertificate().getPublicKey()));
            DERSequence subjectAlternativeNames = new DERSequence(new ASN1Encodable[]{
                    new GeneralName(GeneralName.dNSName, "localhost"),
                    new GeneralName(GeneralName.dNSName, "127.0.0.1")
            });
            builder.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);

            // Sign by CA
            X509Certificate cert = signCertificate(builder);
            return new X500PrivateCredential(cert, kp.getPrivate());
        } catch (CertificateException e) {
            throw new MisconfigurationException("", e);
        } catch (CertIOException e) {
            throw new MisconfigurationException("", e);
        } catch (NoSuchAlgorithmException e) {
            throw new MisconfigurationException("", e);
        } catch (OperatorCreationException e) {
            throw new MisconfigurationException("", e);
        }
    }

    public X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder) throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder(config.getCertConfiguration().getSignAlgorithm())
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(ca.getPrivateKey());
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(signer));
    }


    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<CertificateProvider>() {
            @Override
            public void start(CertificateProvider component) {
                config = getDependency(BouncrConfiguration.class);
                expiry = config.getCertConfiguration().getDefaultExpiry();
                flake = getDependency(Flake.class);
            }

            @Override
            public void stop(CertificateProvider component) {

            }
        };
    }

    public void setCA(X500PrivateCredential ca) {
        this.ca = ca;
    }
}
