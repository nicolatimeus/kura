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
package org.eclipse.kura.remoteservice.discovery.fs.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class KuraServicePidFilteringDispatcher extends EndpointEventListenerDispatcher {

    private final Map<String, Set<EndpointDescription>> endpointsByKuraServicePid = new HashMap<>();
    private final Set<String> allActiveLocalPids = new HashSet<>();
    private final ServiceTracker<Object, String> tracker;

    public KuraServicePidFilteringDispatcher(final BundleContext context) {

        final Filter filter;

        try {
            filter = FrameworkUtil.createFilter("(&(" + ConfigurationService.KURA_SERVICE_PID + "=*)(!("
                    + RemoteConstants.SERVICE_IMPORTED + "=*)))");
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("unreachable");
        }

        tracker = new ServiceTracker<>(context, filter, new ServiceTrackerCustomizer<Object, String>() {

            @Override
            public String addingService(ServiceReference<Object> service) {
                final String pid = (String) service.getProperty(ConfigurationService.KURA_SERVICE_PID);

                allActiveLocalPids.add(pid);
                onLocalPidAdded(pid);

                return pid;
            }

            @Override
            public void modifiedService(ServiceReference<Object> service, String pid) {
                // no need
            }

            @Override
            public void removedService(ServiceReference<Object> service, String pid) {

                allActiveLocalPids.remove(pid);
                onLocalPidRemoved(pid);

            }

        });

        tracker.open();
    }

    private synchronized void onLocalPidAdded(final String pid) {
        final Set<EndpointDescription> endpoints = this.endpointsByKuraServicePid.get(pid);

        if (endpoints == null) {
            return;
        }

        for (final EndpointDescription endpoint : endpoints) {
            super.endpointRemoved(Utils.getId(endpoint));
        }

    }

    private synchronized void onLocalPidRemoved(final String pid) {
        final Set<EndpointDescription> endpoints = this.endpointsByKuraServicePid.get(pid);

        if (endpoints == null) {
            return;
        }

        for (final EndpointDescription endpoint : endpoints) {
            super.endpointChanged(endpoint);
        }
    }

    @Override
    public synchronized void endpointChanged(EndpointDescription endpoint) {

        final Object kuraServicePid = endpoint.getProperties().get(ConfigurationService.KURA_SERVICE_PID);

        if (kuraServicePid instanceof String) {
            final String asString = (String) kuraServicePid;

            this.endpointsByKuraServicePid.compute(asString, (k, v) -> {
                if (v == null) {
                    v = new HashSet<>();
                }

                v.add(endpoint);

                return v;
            });

            if (allActiveLocalPids.contains(asString)) {
                super.endpointRemoved(Utils.getId(endpoint));
            } else {
                super.endpointChanged(endpoint);
            }

        } else {
            super.endpointChanged(endpoint);
        }

    }

    @Override
    public synchronized void endpointRemoved(String id) {
        final EndpointDescription endpoint = super.endpointDescriptions.get(id);

        if (endpoint != null) {

            final Object kuraServicePid = endpoint.getProperties().get(ConfigurationService.KURA_SERVICE_PID);

            if (kuraServicePid instanceof String) {

                final String asString = (String) kuraServicePid;

                this.endpointsByKuraServicePid.compute(asString, (k, v) -> {

                    if (v != null) {
                        v.remove(endpoint);
                    }

                    return v;
                });

            }

        }

        super.endpointRemoved(id);
    }

    @Override
    public void close() {
        tracker.close();
    }
}
