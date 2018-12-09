package net.unit8.bouncr.proxy.cert;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.MisconfigurationException;
import enkan.exception.UnreachableException;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

public class ReloadableTrustManager extends SystemComponent implements X509TrustManager {
    private X509TrustManager trustManager;
    private TrustManagerFactory trustManagerFactory;
    private String truststorePath;
    private String truststorePassword;
    private KeyStore trustStore;

    private boolean isInitialized = false;

    private void load() throws CertificateException, NoSuchAlgorithmException, IOException {
        try(InputStream in = new FileInputStream(new File(truststorePath))) {
            trustStore.load(in, truststorePassword.toCharArray());
        }
    }

    public void reload() {
        try {
            trustStore = KeyStore.getInstance("JKS");
            load();
            trustManagerFactory.init(trustStore);
            trustManager = Stream.of(trustManagerFactory.getTrustManagers())
                    .filter(X509TrustManager.class::isInstance)
                    .map(X509TrustManager.class::cast)
                    .findAny()
                    .orElseThrow(() -> new MisconfigurationException(""));
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new UnreachableException(e);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(X509Certificate cert) {
        try(OutputStream out = new FileOutputStream(truststorePath)) {
            trustStore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
            trustStore.store(out, truststorePassword.toCharArray());
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<ReloadableTrustManager>() {
            @Override
            public void start(ReloadableTrustManager component) {
                if (truststorePath != null && !truststorePath.isEmpty()) {
                    try {
                        trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        reload();
                        isInitialized = true;
                    } catch (NoSuchAlgorithmException e) {
                        throw new UnreachableException(e);
                    }
                }
            }

            @Override
            public void stop(ReloadableTrustManager component) {
                isInitialized = false;
            }
        };
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        trustManager.checkClientTrusted(certificates, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        trustManager.checkServerTrusted(certificates, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }

    public boolean initialized() {
        return isInitialized;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }
}
