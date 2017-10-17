package net.unit8.bouncr.ssl;

import lombok.Data;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;

public class BouncrSSLSocketFactory extends SSLSocketFactory {
    private static ThreadLocal<KeyStoreInfo> keyStoreInfo = ThreadLocal.withInitial(() -> new KeyStoreInfo());
    private SSLSocketFactory delegate;

    public BouncrSSLSocketFactory() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance("JKS");

            try (InputStream in = new FileInputStream(keyStoreInfo.get().getTruststorePath())) {
                trustStore.load(in, keyStoreInfo.get().getTruststorePassword().toCharArray());
            }
            tmf.init(trustStore);
            ctx.init(null, tmf.getTrustManagers(), SecureRandom.getInstance("SHA1PRNG"));
            delegate = ctx.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Data
    static class KeyStoreInfo implements Serializable {
        private String truststorePath;
        private String truststorePassword;
    }

    public static void setTruststorePath(String truststorePath) {
        keyStoreInfo.get().setTruststorePath(truststorePath);
    }

    public static void setTruststorePassword(String truststorePassword) {
        keyStoreInfo.get().setTruststorePassword(truststorePassword);
    }
    public static SocketFactory getDefault() {
        return new BouncrSSLSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return delegate.createSocket(socket, s, i, b);
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return delegate.createSocket(s, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return delegate.createSocket(s, i, inetAddress, i1);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return delegate.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return delegate.createSocket(inetAddress, i, inetAddress1, i1);
    }
}
