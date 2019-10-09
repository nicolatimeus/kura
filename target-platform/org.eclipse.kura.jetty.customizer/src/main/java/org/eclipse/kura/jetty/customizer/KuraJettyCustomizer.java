/*******************************************************************************
 * Copyright (c) 2018, 2019 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.jetty.customizer;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.SessionCookieConfig;

import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class KuraJettyCustomizer extends JettyCustomizer {

    @Override
    public Object customizeContext(Object context, Dictionary<String, ?> settings) {
        if (!(context instanceof ServletContextHandler)) {
            return context;
        }

        final ServletContextHandler servletContextHandler = (ServletContextHandler) context;

        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setCompressionLevel(9);

        servletContextHandler.setGzipHandler(gzipHandler);

        servletContextHandler.setErrorHandler(new KuraErrorHandler());

        final SessionCookieConfig cookieConfig = servletContextHandler.getSessionHandler().getSessionCookieConfig();

        cookieConfig.setHttpOnly(true);

        return context;
    }

    @Override
    public Object customizeHttpConnector(final Object connector, final Dictionary<String, ?> settings) {
        customizeConnector(connector);
        return connector;
    }

    @Override
    public Object customizeHttpsConnector(final Object connector, final Dictionary<String, ?> settings) {

        customizeConnector(connector);

        final ServerConnector serverConnector = (ServerConnector) connector;

        addClientAuthSslConnector(serverConnector.getServer(), settings);

        return connector;
    }

    private void addClientAuthSslConnector(final Server server, final Dictionary<String, ?> settings) {

        final boolean isRevocationEnabled = (Boolean) settings.get("org.eclipse.kura.revocation.check.enabled");

        final SslContextFactory sslContextFactory = new SslContextFactory() {

            protected PKIXBuilderParameters newPKIXBuilderParameters(KeyStore trustStore,
                    Collection<? extends java.security.cert.CRL> crls) throws Exception {
                PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());

                pbParams.setMaxPathLength(getMaxCertPathLength());
                pbParams.setRevocationEnabled(false);

                if (isEnableOCSP()) {

                    final PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) CertPathValidator
                            .getInstance("PKIX").getRevocationChecker();

                    final String responderURL = getOcspResponderURL();
                    if (responderURL != null) {
                        revocationChecker.setOcspResponder(new URI(responderURL));
                    }
                    revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL,
                            PKIXRevocationChecker.Option.NO_FALLBACK));

                    pbParams.addCertPathChecker(revocationChecker);
                }

                if (getPkixCertPathChecker() != null)
                    pbParams.addCertPathChecker(getPkixCertPathChecker());

                if (crls != null && !crls.isEmpty()) {
                    pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
                }

                if (isEnableCRLDP()) {
                    // Enable Certificate Revocation List Distribution Points (CRLDP) support
                    System.setProperty("com.sun.security.enableCRLDP", "true");
                }

                return pbParams;
            }
        };

        final String keyStorePath = (String) settings.get(JettyConstants.SSL_KEYSTORE);
        final String keyStorePassword = (String) settings.get(JettyConstants.SSL_PASSWORD);

        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setKeyStoreType("JKS");
        sslContextFactory.setProtocol("TLS");
        sslContextFactory.setTrustManagerFactoryAlgorithm("PKIX");

        sslContextFactory.setWantClientAuth(true);
        sslContextFactory.setNeedClientAuth(true);

        sslContextFactory.setEnableOCSP(isRevocationEnabled);
        sslContextFactory.setValidatePeerCerts(isRevocationEnabled);

        if (isRevocationEnabled) {
            final Object ocspURI = settings.get("org.eclipse.kura.revocation.crl.path");
            sslContextFactory.setOcspResponderURL(ocspURI instanceof String ? (String) ocspURI : null);
        }

        final HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        final Set<TrustAnchor> certs = loadCertificates(keyStorePath, keyStorePassword);

        if (!certs.isEmpty()) {
            httpsConfig.addCustomizer(
                    (connector, config, req) -> req.setAttribute("org.eclipse.kura.web.trust.anchors", certs));
        }

        final ServerConnector connector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
        connector.setPort(4443);

        customizeConnector(connector);

        server.addConnector(connector);
    }

    private void customizeConnector(Object connector) {
        if (!(connector instanceof ServerConnector)) {
            return;
        }

        final ServerConnector serverConnector = (ServerConnector) connector;

        addCustomizer(serverConnector, new ForwardedRequestCustomizer());

    }

    private void addCustomizer(final ServerConnector connector, final Customizer customizer) {
        for (final ConnectionFactory factory : connector.getConnectionFactories()) {
            if (!(factory instanceof HttpConnectionFactory)) {
                continue;
            }

            final HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) factory;

            httpConnectionFactory.getHttpConfiguration().setSendServerVersion(false);

            List<Customizer> customizers = httpConnectionFactory.getHttpConfiguration().getCustomizers();
            if (customizers == null) {
                customizers = new LinkedList<>();
                httpConnectionFactory.getHttpConfiguration().setCustomizers(customizers);
            }

            customizers.add(customizer);
        }
    }

    private Set<TrustAnchor> loadCertificates(final String keyStorePath, final String keyStorePassword) {
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");

            try (final FileInputStream in = new FileInputStream(keyStorePath)) {
                keyStore.load(in, keyStorePassword.toCharArray());
            }

            final Set<TrustAnchor> result = new HashSet<>();

            final Enumeration<String> aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();

                final Certificate cert = keyStore.getCertificate(alias);

                if (!(cert instanceof X509Certificate)) {
                    continue;
                }

                if (keyStore.isCertificateEntry(alias)) {
                    result.add(new TrustAnchor((X509Certificate) cert, null));
                }
            }

            return result;
        } catch (final Exception e) {
            return Collections.emptySet();
        }
    }

}
