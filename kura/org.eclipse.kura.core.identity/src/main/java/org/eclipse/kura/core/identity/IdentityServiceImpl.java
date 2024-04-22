/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.identity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordHash;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.identity.configuration.extension.IdentityConfigurationExtension;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class IdentityServiceImpl implements IdentityService {

    private static final String IDENTITY_SERVICE_FAILURE_FORMAT_STRING = "{} IdentityService - Failure - {}";
    private static final String IDENTITY_SERVICE_SUCCESS_FORMAT_STRING = "{} IdentityService - Success - {}";

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");
    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceImpl.class);

    private static final String PASSWORD_PROPERTY = "kura.password";
    private static final String KURA_NEED_PASSWORD_CHANGE = "kura.need.password.change";

    private UserAdmin userAdmin;
    private CryptoService cryptoService;
    private UserAdminHelper userAdminHelper;

    private final Map<String, IdentityConfigurationExtension> extensions = new ConcurrentHashMap<>();

    public void setCryptoService(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    public synchronized void setIdentityConfigurationExtension(
            final IdentityConfigurationExtension identityConfigurationExtension, final Map<String, Object> properties) {
        final Object kuraServicePid = properties.get(ConfigurationService.KURA_SERVICE_PID);

        if (!(kuraServicePid instanceof String)) {
            logger.warn("found {} registered without setting the {} service property, service will not be tracked",
                    IdentityConfigurationExtension.class.getSimpleName(), ConfigurationService.KURA_SERVICE_PID);
            return;
        }

        this.extensions.put((String) kuraServicePid, identityConfigurationExtension);
    }

    public synchronized void unsetIdentityConfigurationExtension(
            final IdentityConfigurationExtension identityConfigurationExtension, final Map<String, Object> properties) {
        final Object kuraServicePid = properties.get(ConfigurationService.KURA_SERVICE_PID);

        if (kuraServicePid instanceof String) {
            this.extensions.remove(kuraServicePid);
        }
    }

    public void activate() {
        this.userAdminHelper = new UserAdminHelper(this.userAdmin, this.cryptoService);
    }

    @Override
    public synchronized boolean createIdentity(final String name) throws KuraException {
        if (this.userAdminHelper.getUser(name).isPresent()) {
            return false;
        }

        audit(() -> this.userAdminHelper.createUser(name), "Create identity " + name);
        return true;
    }

    @Override
    public synchronized boolean deleteIdentity(final String name) throws KuraException {
        if (!this.userAdminHelper.getUser(name).isPresent()) {
            return false;
        }

        audit(() -> this.userAdminHelper.deleteUser(name), "Delete identity " + name);
        return true;
    }

    @Override
    public synchronized List<IdentityConfiguration> getIdentitiesConfiguration(
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws KuraException {
        final List<IdentityConfiguration> result = new ArrayList<>();

        this.userAdminHelper.foreachUser((name, user) -> result.add(buildIdentity(name, user, componentsToReturn)));

        return result;
    }

    @Override
    public synchronized Optional<IdentityConfiguration> getIdentityConfiguration(String name,
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws KuraException {

        return this.userAdminHelper.getUser(name).map(u -> buildIdentity(name, u, componentsToReturn));
    }

    @Override
    public IdentityConfiguration getIdentityDefaultConfiguration(String identityName,
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws KuraException {
        final List<IdentityConfigurationComponent> components = new ArrayList<>();

        if (componentsToReturn.contains(PasswordConfiguration.class)) {
            components.add(new PasswordConfiguration(false, false, Optional.empty()));
        }

        if (componentsToReturn.contains(AssignedPermissions.class)) {
            components.add(new AssignedPermissions(Collections.emptySet()));
        }

        if (componentsToReturn.contains(AdditionalConfigurations.class)) {
            components.add(getAdditionalConfigurationsDefaults(identityName));
        }

        return new IdentityConfiguration(identityName, components);
    }

    @Override
    public void validateIdentityConfigurations(Collection<IdentityConfiguration> identityConfigurations)
            throws KuraException {
        final FailureHandler failureHandler = new FailureHandler();

        for (final IdentityConfiguration configuration : identityConfigurations) {
            validateIdentityConfiguration(configuration, failureHandler);
        }

        audit(() -> failureHandler.throwIfFailuresOccurred(), "Validate identity configuration");
    }

    @Override
    public synchronized void updateIdentityConfigurations(
            final Collection<IdentityConfiguration> identityConfigurations) throws KuraException {

        final FailureHandler failureHandler = new FailureHandler();

        final Map<String, User> users = new HashMap<>();

        for (final IdentityConfiguration configuration : identityConfigurations) {
            final Optional<User> user = this.userAdminHelper.getUser(configuration.getName());

            if (!user.isPresent()) {
                failureHandler.addError(configuration.getName(), "identity does not exist");
                continue;
            }

            users.put(configuration.getName(), user.get());
        }

        for (final IdentityConfiguration configuration : identityConfigurations) {
            validateIdentityConfiguration(configuration, failureHandler);
        }

        audit(() -> failureHandler.throwIfFailuresOccurred(), "Validate updated identity configuration");

        for (final IdentityConfiguration configuration : identityConfigurations) {
            updateIdentityConfigurationInternal(users.get(configuration.getName()), configuration, failureHandler);
        }

        failureHandler.throwIfFailuresOccurred();
    }

    @Override
    public synchronized boolean createPermission(final Permission permission) throws KuraException {
        if (this.userAdminHelper.getPermission(permission.getName()).isPresent()) {
            return false;
        }

        this.userAdminHelper.getOrCreatePermission(permission.getName());
        return true;
    }

    @Override
    public synchronized boolean deletePermission(final Permission permission) throws KuraException {
        if (!this.userAdminHelper.getPermission(permission.getName()).isPresent()) {
            return false;
        }

        this.userAdminHelper.deletePremission(permission.getName());
        return true;
    }

    @Override
    public synchronized Set<Permission> getPermissions() {
        return new UserAdminHelper(this.userAdmin, this.cryptoService).getDefinedPermissions().stream()
                .map(Permission::new).collect(Collectors.toSet());
    }

    @Override
    public PasswordHash computePasswordHash(final char[] password) throws KuraException {
        try {
            final String result = this.cryptoService.sha256Hash(new String(password));

            for (int i = 0; i < password.length; i++) {
                password[i] = ' ';
            }

            return new PasswordHashImpl(result);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, "failed to compute password hash");
        }
    }

    private void updateIdentityConfigurationInternal(final User user, final IdentityConfiguration identity,
            final FailureHandler failureHandler) {

        final String identityName = identity.getName();
        final Optional<PasswordConfiguration> passwordData = identity.getComponent(PasswordConfiguration.class);

        if (passwordData.isPresent()) {
            updatePassword(identityName, passwordData.get(), user);
        }

        final Optional<AssignedPermissions> permissions = identity.getComponent(AssignedPermissions.class);

        if (permissions.isPresent()) {
            updateAssignedPermissions(identityName, permissions.get(), user);
        }

        final Optional<AdditionalConfigurations> additionalConfigurations = identity
                .getComponent(AdditionalConfigurations.class);

        if (additionalConfigurations.isPresent()) {
            updateAdditionalConfigurations(identityName, additionalConfigurations.get(), failureHandler);
        }
    }

    private IdentityConfiguration buildIdentity(final String name, final User user,
            Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) {

        final List<IdentityConfigurationComponent> components = new ArrayList<>();

        if (componentsToReturn.contains(PasswordConfiguration.class)) {
            components.add(getPasswordData(user));
        }

        if (componentsToReturn.contains(AssignedPermissions.class)) {
            final Set<Permission> permissions = this.userAdminHelper.getIdentityPermissions(name).stream()
                    .map(Permission::new).collect(Collectors.toSet());
            components.add(new AssignedPermissions(permissions));
        }

        if (componentsToReturn.contains(AdditionalConfigurations.class)) {
            components.add(getAdditionalConfigurations(name));
        }

        return new IdentityConfiguration(name, components);
    }

    private AdditionalConfigurations getAdditionalConfigurations(final String name) {
        final List<ComponentConfiguration> additionalConfigurations = new ArrayList<>();

        for (final IdentityConfigurationExtension extension : this.extensions.values()) {
            try {
                extension.getConfiguration(name).ifPresent(additionalConfigurations::add);
            } catch (final Exception ex) {
                logger.warn("failed to get identity additional configuration from extension", ex);
            }
        }

        return new AdditionalConfigurations(additionalConfigurations);
    }

    private AdditionalConfigurations getAdditionalConfigurationsDefaults(final String name) {
        final List<ComponentConfiguration> additionalConfigurations = new ArrayList<>();

        for (final IdentityConfigurationExtension extension : this.extensions.values()) {
            try {
                extension.getDefaultConfiguration(name).ifPresent(additionalConfigurations::add);
            } catch (final Exception e) {
                logger.warn("failed to get identity additional configuration defaults from extension", e);
            }
        }

        return new AdditionalConfigurations(additionalConfigurations);
    }

    private PasswordConfiguration getPasswordData(final User user) {
        final Optional<String> passwordHash = Optional.ofNullable(user.getCredentials().get(PASSWORD_PROPERTY))
                .filter(String.class::isInstance).map(String.class::cast);

        final boolean isPasswordChangeNeeded = Objects.equals("true",
                user.getProperties().get(KURA_NEED_PASSWORD_CHANGE));

        return new PasswordConfiguration(isPasswordChangeNeeded, passwordHash.isPresent(),
                passwordHash.map(PasswordHashImpl::new));
    }

    private void setProperty(final Dictionary<String, Object> properties, final String key, final Object value) {
        if (!Objects.equals(properties.get(key), value)) {
            properties.put(key, value);
        }
    }

    private void removeProperty(final Dictionary<String, Object> properties, final String key) {
        if (properties.get(key) != null) {
            properties.remove(key);
        }
    }

    private void updateAssignedPermissions(final String identityName, final AssignedPermissions assignedPermissions,
            final User user) {
        this.userAdminHelper.foreachPermission((name, group) -> {
            final Permission permission = new Permission(name);
            final List<Role> members = Optional.ofNullable(group.getMembers()).map(Arrays::asList)
                    .orElse(Collections.emptyList());

            if (assignedPermissions.getPermissions().contains(permission) && !members.contains(user)) {
                audit(() -> group.addMember(user),
                        "Add permission " + permission.getName() + " to identity " + identityName);
            } else if (!assignedPermissions.getPermissions().contains(permission) && members.contains(user)) {
                audit(() -> group.removeMember(user),
                        "Remove permission " + permission.getName() + " from identity " + identityName);
            }
        });
    }

    private void updatePassword(final String identityName, final PasswordConfiguration passwordData, final User user) {
        final Dictionary<String, Object> properties = user.getProperties();

        final Object currentIsPasswordChangeNeeded = properties.get(KURA_NEED_PASSWORD_CHANGE);

        if (passwordData.isPasswordChangeNeeded()) {
            if (!"true".equals(currentIsPasswordChangeNeeded)) {
                audit(() -> setProperty(properties, KURA_NEED_PASSWORD_CHANGE, "true"),
                        "Enable password change at next login for identity " + identityName);
            }
        } else if (currentIsPasswordChangeNeeded != null) {
            audit(() -> removeProperty(properties, KURA_NEED_PASSWORD_CHANGE),
                    "Disable password change at next login for identity " + identityName);
        }

        final Dictionary<String, Object> credentials = user.getCredentials();
        final Optional<PasswordHash> hash = passwordData.getPasswordHash();

        final Object currentPasswordHash = credentials.get(PASSWORD_PROPERTY);

        if (passwordData.isPasswordAuthEnabled() && hash.isPresent()) {

            audit(() -> setProperty(credentials, PASSWORD_PROPERTY, hash.get().toString()),
                    "Update Kura password for identity " + identityName);

        } else if (!passwordData.isPasswordAuthEnabled() && currentPasswordHash != null) {
            audit(() -> removeProperty(credentials, PASSWORD_PROPERTY),
                    "Disable Kura password for identity " + identityName);
        }
    }

    private void validateIdentityConfiguration(final IdentityConfiguration identityConfiguration,
            final FailureHandler failureHandler) throws KuraException {
        final Optional<AdditionalConfigurations> additionalConfigurations = identityConfiguration
                .getComponent(AdditionalConfigurations.class);

        if (additionalConfigurations.isPresent()) {
            for (final ComponentConfiguration config : additionalConfigurations.get().getConfigurations()) {

                final Optional<IdentityConfigurationExtension> extension = Optional
                        .ofNullable(this.extensions.get(config.getPid()));

                if (!extension.isPresent()) {
                    failureHandler.addError(identityConfiguration.getName(),
                            "extension " + config.getPid() + " is not registered");
                    continue;
                }

                try {
                    extension.get().validateConfiguration(identityConfiguration.getName(), config);
                } catch (final Exception e) {
                    failureHandler.addError(identityConfiguration.getName(), e.getMessage());
                }
            }
        }

        final Optional<AssignedPermissions> assignedPermissions = identityConfiguration
                .getComponent(AssignedPermissions.class);

        if (assignedPermissions.isPresent()) {
            for (final Permission permission : assignedPermissions.get().getPermissions()) {
                if (!this.userAdminHelper.getPermission(permission.getName()).isPresent()) {
                    failureHandler.addError(identityConfiguration.getName(),
                            "permission " + permission.getName() + " does not exists");
                }
            }
        }
    }

    private void updateAdditionalConfigurations(final String identityName,
            final AdditionalConfigurations additionalConfigurations, final FailureHandler failureHandler) {

        for (final ComponentConfiguration config : additionalConfigurations.getConfigurations()) {
            final String pid = config.getPid();

            final Optional<IdentityConfigurationExtension> extension = Optional.ofNullable(this.extensions.get(pid));

            if (!extension.isPresent()) {
                failureHandler.addError(identityName, "extension " + pid + " is not registered");
                continue;
            }

            try {
                audit(() -> extension.get().updateConfiguration(identityName, config),
                        "Update configuration for extension " + pid + " for identity " + identityName);
            } catch (final KuraException e) {
                failureHandler.addError(identityName, e.getMessage());
            }
        }
    }

    private static class FailureHandler {

        final Set<String> errors = new HashSet<>();

        public void addError(final String message) {
            this.errors.add(message);
            logger.error(message);
        }

        public void addError(final String identity, final String message) {
            addError(identity + ": " + message);
        }

        public void throwIfFailuresOccurred() throws KuraException {
            if (!this.errors.isEmpty()) {

                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                        this.errors.stream().collect(Collectors.joining("; ")));
            }
        }
    }

    private static <T, E extends Throwable> T audit(final FallibleSupplier<T, E> task, final String message) throws E {
        try {
            final T result = task.get();
            auditLogger.info(IDENTITY_SERVICE_SUCCESS_FORMAT_STRING, AuditContext.currentOrInternal(), message);
            return result;
        } catch (final Exception e) {
            auditLogger.warn(IDENTITY_SERVICE_FAILURE_FORMAT_STRING, AuditContext.currentOrInternal(), message);
            throw e;
        }
    }

    private static <E extends Throwable> void audit(final FallibleTask<E> task, final String message) throws E {
        try {
            task.run();
            auditLogger.info(IDENTITY_SERVICE_SUCCESS_FORMAT_STRING, AuditContext.currentOrInternal(), message);
        } catch (final Exception e) {
            auditLogger.warn(IDENTITY_SERVICE_FAILURE_FORMAT_STRING, AuditContext.currentOrInternal(), message);
            throw e;
        }
    }

    private interface FallibleSupplier<T, E extends Throwable> {

        public T get() throws E;
    }

    private interface FallibleTask<E extends Throwable> {

        public void run() throws E;
    }
}
