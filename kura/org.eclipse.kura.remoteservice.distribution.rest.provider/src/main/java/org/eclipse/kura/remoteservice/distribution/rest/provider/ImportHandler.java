/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.remoteservice.distribution.rest.provider;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.objectweb.asm.Opcodes;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.client.WebTarget;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class ImportHandler {

    private final Collection<?> providers;

    public ImportHandler(final Collection<?> providers) {
        this.providers = providers;
    }

    private static final Logger logger = LoggerFactory.getLogger(ImportHandler.class);

    public Object importEndpoint(final ClassLoader cl, final BundleContext consumerContext, final Class<?>[] interfaces,
            final EndpointDescription endpoint) {

        try {

            final Map<String, Object> properties = endpoint.getProperties();

            final Optional<KuraEndpointId> kuraId = extractProperty(properties, RemoteConstants.ENDPOINT_ID)
                    .flatMap(KuraEndpointId::fromJson);

            if (!kuraId.isPresent()) {
                return null;
            }

            final JerseyClientBuilder clientBuilder = new JerseyClientBuilder();

            for (final Object provider : providers) {
                clientBuilder.register(provider);
            }

            final ByteBuddy byteBuddy = new ByteBuddy();

            Builder<Object> builder = byteBuddy.subclass(Object.class);

            int i = 0;

            for (final Class<?> intf : interfaces) {

                final String fieldName = "proxy" + i;

                builder = builder.implement(intf).method(ElementMatchers.isDeclaredBy(intf))
                        .intercept(MethodDelegation.toField(fieldName))
                        .defineField(fieldName, intf, Opcodes.ACC_PUBLIC);
                i++;
            }

            final Class<?> resultClass = builder.make().load(cl).getLoaded();
            final Object result = resultClass.getConstructor().newInstance();

            i = 0;

            for (final Class<?> intf : interfaces) {

                final WebTarget target = clientBuilder.hostnameVerifier((s1, s2) -> true)
                        .sslContext(trustAllSSLContext()).build().target(kuraId.get().getBaseURL() + "/"
                                + intf.getName().replaceAll("[.]", "/") + "/" + kuraId.get().getId());

                final Object proxy = WebResourceFactory.newResource(intf, target);
                final String fieldName = "proxy" + i;

                resultClass.getField(fieldName).set(result, proxy);

                i++;
            }

            return result;
        } catch (final Exception e) {
            logger.warn("failed to import {}", endpoint, e);
            return null;
        }
    }

    private static Optional<String> extractProperty(final Map<String, Object> properties, final String key) {
        final Object result = properties.get(key);

        if (!(result instanceof String)) {
            return Optional.empty();
        }

        return Optional.of((String) result);
    }

    private SSLContext trustAllSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(null, new TrustManager[] { new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // trust all
            }

            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // trust all
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } }, new SecureRandom());
        return sslContext;
    }
}
