/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.useradmin.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.felix.useradmin.RoleFactory;
import org.apache.felix.useradmin.RoleRepositoryStore;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

public class RoleRepositoryStoreImpl implements RoleRepositoryStore, UserAdminListener, ConfigurableComponent {

    private static final String INTERNAL_UPDATE_ID_PROP_NAME = "internal.update.id";

    private static final Logger logger = LoggerFactory.getLogger(RoleRepositoryStoreImpl.class);

    private Map<String, Role> roles = new HashMap<>();
    private RoleRepositoryStoreOptions options;

    long nextUpdateId = 0;
    private final Set<Long> updateIds = new HashSet<>();

    private ConfigurationService configurationService;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Optional<ScheduledFuture<?>> storeTask = Optional.empty();

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        doUpdate(properties);

        logger.info("activating...done");
    }

    public synchronized void update(final Map<String, Object> properties) {

        if (isSelfUpdate(properties)) {
            logger.info("ignoring self update");
            return;
        }

        logger.info("updating...");

        doUpdate(properties);

        logger.info("updating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        executorService.shutdown();

        logger.info("deactivating...done");
    }

    @Override
    public synchronized Role addRole(final String name, final int type) throws Exception {

        if (roles.containsKey(name)) {
            return null;
        }

        final Role role = RoleFactory.createRole(type, name);

        roles.put(name, role);

        return role;
    }

    @Override
    public synchronized Role getRoleByName(final String name) throws Exception {
        return roles.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized Role[] getRoles(final String filterString) throws Exception {

        final Optional<Filter> filter;

        if (filterString != null) {
            filter = Optional.of(FrameworkUtil.createFilter(filterString));
        } else {
            filter = Optional.empty();
        }

        final List<Role> result = new ArrayList<>();

        for (final Role role : roles.values()) {
            if (!filter.isPresent() || filter.get().match(role.getProperties())) {
                result.add(role);
            }
        }

        return result.toArray(new Role[result.size()]);
    }

    @Override
    public synchronized Role removeRole(final String role) throws Exception {
        return Optional.ofNullable(roles.remove(role)).orElse(null);
    }

    private boolean isSelfUpdate(final Map<String, Object> properties) {
        final Object id = properties.get(INTERNAL_UPDATE_ID_PROP_NAME);

        if (id instanceof Long) {
            final long updateId = (Long) id;

            if (updateIds.contains(updateId)) {
                updateIds.remove(updateId);
                return true;
            }
        }

        return false;
    }

    public long getNextUpdateId() {
        final long updateId = nextUpdateId++;

        updateIds.add(updateId);

        return updateId;
    }

    private void doUpdate(final Map<String, Object> properties) {
        options = new RoleRepositoryStoreOptions(properties);

        try {
            roles = decode(options);
        } catch (final Exception e) {
            logger.warn("failed to deserialize roles", e);
        }
    }

    private synchronized void scheduleStore() {

        if (storeTask.isPresent()) {
            storeTask.get().cancel(false);
            storeTask = Optional.empty();
        }

        storeTask = Optional
                .of(executorService.schedule(this::storeNow, options.getWriteDelayMs(), TimeUnit.MILLISECONDS));
    }

    private synchronized void storeNow() {
        try {
            final JsonArray rolesArray = new JsonArray();
            final JsonArray usersArray = new JsonArray();
            final JsonArray groupsArray = new JsonArray();

            for (final Role role : roles.values()) {
                final int type = role.getType();

                if (type == Role.ROLE) {
                    rolesArray.add(RoleSerializer.serializeRole(role));
                } else if (type == Role.USER) {
                    usersArray.add(RoleSerializer.serializeRole(role));
                } else if (type == Role.GROUP) {
                    groupsArray.add(RoleSerializer.serializeRole(role));
                }
            }

            final Map<String, Object> properties = new RoleRepositoryStoreOptions(rolesArray.toString(), //
                    usersArray.toString(), //
                    groupsArray.toString(), //
                    options.getWriteDelayMs() //
            ).toProperties();

            properties.put(INTERNAL_UPDATE_ID_PROP_NAME, getNextUpdateId());

            configurationService.updateConfiguration(RoleRepositoryStoreImpl.class.getName(), properties);
        } catch (final Exception e) {
            logger.warn("Failed to store configuration", e);
        } finally {
            storeTask = Optional.empty();
        }

    }

    private final Map<String, Role> decode(final RoleRepositoryStoreOptions options) throws DeserializationException {
        try {
            final Map<String, Role> result = new HashMap<>();

            decode(Json.parse(options.getRolesConfig()).asArray(), Role.class, result);
            decode(Json.parse(options.getUsersConfig()).asArray(), User.class, result);

            final JsonArray groups = Json.parse(options.getGroupsConfig()).asArray();

            decode(groups, Group.class, result);

            for (final JsonValue member : groups) {
                RoleSerializer.assignMembers(member.asObject(), result);
            }

            return result;

        } catch (final DeserializationException e) {
            throw e;
        } catch (final Exception e) {
            throw new DeserializationException("failed to deserialize role repository", e);
        }

    }

    private void decode(final JsonArray array, final Class<? extends Role> classz, final Map<String, Role> target)
            throws DeserializationException {
        for (final JsonValue member : array) {
            final Role role = RoleSerializer.deserializeRole(classz, member.asObject());
            target.put(role.getName(), role);
        }
    }

    @Override
    public void roleChanged(UserAdminEvent arg0) {
        logger.info("received event");
        scheduleStore();
    }

}