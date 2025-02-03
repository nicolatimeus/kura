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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointEventListenerDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(EndpointEventListenerDispatcher.class);

    protected final Map<String, EndpointDescription> endpointDescriptions = new HashMap<>();
    protected final Map<EndpointEventListener, FilterState> listenerState = new HashMap<>();

    public synchronized void listenerChanged(final EndpointEventListener listener, final List<String> filterStrings) {
        final Set<Filter> filters = new HashSet<>();

        for (final String filterString : filterStrings) {

            try {
                filters.add(FrameworkUtil.createFilter(filterString));
            } catch (final InvalidSyntaxException e) {
                logger.warn("invalid filters syntax", e);
            }

        }

        final Map<String, MatchedEndpoint> matchingEndpoints = getMatchingEndpoints(filters);

        this.listenerState.compute(listener, (l, existingState) -> {

            if (existingState == null) {
                existingState = new FilterState(Collections.emptySet(), Collections.emptyMap());
            }

            dispatchUpdates(existingState, listener, matchingEndpoints);

            return new FilterState(filters, matchingEndpoints);

        });

    }

    public synchronized void removeListener(final EndpointEventListener listener) {
        this.listenerState.remove(listener);
    }

    public synchronized void endpointChanged(final EndpointDescription endpoint) {
        for (final Entry<EndpointEventListener, FilterState> e : listenerState.entrySet()) {

            final String id = Utils.getId(endpoint);

            final Optional<Filter> matchingFilter = match(e.getValue().filters, endpoint);

            if (matchingFilter.isPresent()) {
                final MatchedEndpoint existing = e.getValue().matchedEndpoints.get(id);

                final int eventType;

                if (existing != null) {
                    eventType = EndpointEvent.MODIFIED;
                } else {
                    eventType = EndpointEvent.ADDED;
                }

                dispatchEvent(e.getKey(), endpoint, eventType, matchingFilter.get().toString());

                e.getValue().matchedEndpoints.put(id, new MatchedEndpoint(matchingFilter.get(), endpoint));

            } else {
                final MatchedEndpoint existing = e.getValue().matchedEndpoints.remove(id);

                if (existing != null) {
                    dispatchEvent(e.getKey(), endpoint, EndpointEvent.MODIFIED_ENDMATCH, existing.filter.toString());
                }
            }
        }

        this.endpointDescriptions.put(Utils.getId(endpoint), endpoint);
    }

    public synchronized void endpointRemoved(final String id) {

        for (final Entry<EndpointEventListener, FilterState> e : listenerState.entrySet()) {
            final MatchedEndpoint matched = e.getValue().matchedEndpoints.remove(id);

            if (matched != null) {
                dispatchEvent(e.getKey(), matched.endpoint, EndpointEvent.REMOVED, matched.filter.toString());
            }
        }

        endpointDescriptions.remove(id);
    }

    public void close() {
    }

    private Optional<Filter> match(final Collection<Filter> filters, final EndpointDescription endpoint) {
        for (final Filter f : filters) {
            if (f.match(FrameworkUtil.asDictionary(endpoint.getProperties()))) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    private Map<String, MatchedEndpoint> getMatchingEndpoints(final Collection<Filter> filters) {
        final Map<String, MatchedEndpoint> result = new HashMap<>();

        for (final EndpointDescription endpoint : this.endpointDescriptions.values()) {
            final Optional<Filter> matching = match(filters, endpoint);

            if (matching.isPresent()) {
                result.put(Utils.getId(endpoint), new MatchedEndpoint(matching.get(), endpoint));
            }
        }

        return result;
    }

    private void dispatchUpdates(final FilterState existing, final EndpointEventListener listener,
            final Map<String, MatchedEndpoint> newMatchingEndpoints) {

        for (final Entry<String, MatchedEndpoint> e : newMatchingEndpoints.entrySet()) {
            if (!existing.matchedEndpoints.containsKey(e.getKey())) {
                dispatchEvent(listener, e.getValue().endpoint, EndpointEvent.ADDED, e.getValue().filter.toString());
            }
        }

        for (final Entry<String, MatchedEndpoint> e : existing.matchedEndpoints.entrySet()) {
            if (!newMatchingEndpoints.containsKey(e.getKey())) {
                dispatchEvent(listener, endpointDescriptions.get(e.getKey()), EndpointEvent.MODIFIED_ENDMATCH,
                        e.getValue().filter.toString());
            }
        }

    }

    private void dispatchEvent(final EndpointEventListener listener, final EndpointDescription endpoint,
            final int eventType, final String filter) {
        try {
            final EndpointEvent event = new EndpointEvent(eventType, endpoint);

            logger.debug("dispatching {} to {}", event, listener);

            listener.endpointChanged(event, filter);
        } catch (final Exception e) {
            logger.warn("unexpected exception dispatching EndpointEvent", e);
        }
    }

    private static class MatchedEndpoint {

        private final Filter filter;
        private final EndpointDescription endpoint;

        public MatchedEndpoint(final Filter filter, final EndpointDescription endpoint) {
            this.filter = filter;
            this.endpoint = endpoint;
        }

    }

    private static class FilterState {

        final Set<Filter> filters;
        final Map<String, MatchedEndpoint> matchedEndpoints;

        public FilterState(Set<Filter> filters, Map<String, MatchedEndpoint> matchedEndpoints) {
            this.filters = filters;
            this.matchedEndpoints = matchedEndpoints;
        }

    }

}
