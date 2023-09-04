/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.provider;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RestService
        implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);

    private CryptoService cryptoService;
    private UserAdmin userAdmin;

    RestServiceOptions options;

    private final List<ServiceRegistration<?>> registeredServices = new ArrayList<>();
    private final IncomingPortCheckFilter incomingPortCheckFilter = new IncomingPortCheckFilter();
    private final AuthenticationFilter authenticationFilter = new AuthenticationFilter();
    private ServiceTracker<Object, ServiceRegistration<?>> compatModeTracker;

    private AuthenticationProvider passwordAuthProvider;
    private AuthenticationProvider certificateAuthProvider;
    private ConfigurationAdmin configurationAdmin;

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.authenticationFilter.setUserAdmin(userAdmin);
        this.userAdmin = userAdmin;
    }

    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void bindAuthenticationProvider(final AuthenticationProvider authenticationProvider) {
        this.authenticationFilter.registerAuthenticationProvider(authenticationProvider);
    }

    public void unbindAuthenticationProvider(final AuthenticationProvider authenticationProvider) {
        this.authenticationFilter.unregisterAuthenticationProvider(authenticationProvider);
    }

    public void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("activating...");

        final BundleContext bundleContext = componentContext.getBundleContext();

        try {
            compatModeTracker = new ServiceTracker<>(
                    bundleContext,
                    FrameworkUtil.createFilter("(objectClass=*)"), new CompatPublisher(bundleContext));
        } catch (final InvalidSyntaxException e) {
            // no need
        }

        configureDefaultWhiteboard();

        final Dictionary<String, Object> serviceProperties = RestServiceUtils.extensionProperties();

        registeredServices
                .add(bundleContext.registerService(ContainerRequestFilter.class, this.incomingPortCheckFilter,
                        serviceProperties));
        registeredServices
                .add(bundleContext.registerService(ContainerRequestFilter.class, this.authenticationFilter,
                        serviceProperties));
        registeredServices
                .add(bundleContext.registerService(ContainerRequestFilter.class, new AuthorizationFilter(),
                        serviceProperties));
        registeredServices
                .add(bundleContext.registerService(ContainerResponseFilter.class, new AuditFilter(),
                        serviceProperties));
        registeredServices
                .add(bundleContext.registerService(
                        new String[] { MessageBodyWriter.class.getName(), MessageBodyReader.class.getName() },
                        new GsonSerializer<Object>(),
                        serviceProperties));

        this.passwordAuthProvider = new PasswordAuthenticationProvider(bundleContext, userAdmin, cryptoService);
        this.certificateAuthProvider = new CertificateAuthenticationProvider(userAdmin);

        update(properties);

        logger.info("activating...done");
    }

    private void configureDefaultWhiteboard() {
        try {
            final Configuration config = this.configurationAdmin
                    .getConfiguration("org.apache.aries.jax.rs.whiteboard.default");
            final Dictionary<String, Object> configProperties = new Hashtable<>();
            configProperties.put("enabled", true);
            configProperties.put("default.application.base", "/services");
            config.update(configProperties);
        } catch (final Exception e) {
            logger.warn("failed to configure default whiteboard", e);
        }
    }

    public void update(final Map<String, Object> properties) {
        logger.info("updating...");

        final RestServiceOptions newOptions = new RestServiceOptions(properties);

        this.incomingPortCheckFilter.setAllowedPorts(newOptions.getAllowedPorts());

        if (!Objects.equals(this.options, newOptions)) {
            this.options = newOptions;
            updateBuiltinAuthenticationProviders(newOptions);
            if (newOptions.isCompatMode()) {
                this.compatModeTracker.open();
            } else {
                this.compatModeTracker.close();
            }
        }

        logger.info("updating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        for (final ServiceRegistration<?> reg : registeredServices) {
            reg.unregister();
        }

        this.authenticationFilter.close();
        this.compatModeTracker.close();

        logger.info("deactivating...done");
    }

    private void updateBuiltinAuthenticationProviders(final RestServiceOptions options) {
        if (options.isPasswordAuthEnabled()) {
            this.authenticationFilter.registerAuthenticationProvider(this.passwordAuthProvider);
        } else {
            this.authenticationFilter.unregisterAuthenticationProvider(this.passwordAuthProvider);
        }

        if (options.isCertificateAuthEnabled()) {
            this.authenticationFilter.registerAuthenticationProvider(this.certificateAuthProvider);
        } else {
            this.authenticationFilter.unregisterAuthenticationProvider(this.certificateAuthProvider);
        }
    }

}
