package net.unit8.bouncr.cert;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class JdbcTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
