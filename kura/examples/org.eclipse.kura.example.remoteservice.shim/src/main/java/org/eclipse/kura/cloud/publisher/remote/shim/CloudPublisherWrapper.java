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
package org.eclipse.kura.cloud.publisher.remote.shim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.publisher.remote.PublisherApi;
import org.eclipse.kura.cloud.publisher.remote.model.IntegerProperty;
import org.eclipse.kura.cloud.publisher.remote.model.KuraMessage;
import org.eclipse.kura.cloud.publisher.remote.model.LongProperty;
import org.eclipse.kura.cloud.publisher.remote.model.Property;
import org.eclipse.kura.cloud.publisher.remote.model.StringProperty;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.message.KuraPayload;

import jakarta.ws.rs.WebApplicationException;

public class CloudPublisherWrapper implements PublisherApi {

    private final CloudPublisher wrapped;

    public CloudPublisherWrapper(final CloudPublisher wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String publish(final KuraMessage kuraMessage) {

        if (kuraMessage == null) {
            throw new WebApplicationException(400);
        }

        try {
            return this.wrapped.publish(importMessage(kuraMessage));
        } catch (KuraException e) {
            throw new WebApplicationException(500);
        }
    }

    private static final org.eclipse.kura.cloudconnection.message.KuraMessage importMessage(final KuraMessage message) {

        final Map<String, Object> messageProperties = importProperties(message.getProperties());
        final Map<String, Object> kuraPayloadMetrics = Optional.ofNullable(message.getPayload())
                .map(m -> m.getMetrics()).map(CloudPublisherWrapper::importProperties).orElseGet(Collections::emptyMap);

        final KuraPayload payload = new KuraPayload();

        for (final Entry<String, Object> e : kuraPayloadMetrics.entrySet()) {
            payload.addMetric(e.getKey(), e.getValue());
        }

        return new org.eclipse.kura.cloudconnection.message.KuraMessage(payload, messageProperties);
    }

    private static final Map<String, Object> importProperties(final Map<String, Property> properties) {

        final Map<String, Object> result = new HashMap<>();

        if (properties == null) {
            return result;
        }

        for (final Entry<String, Property> e : properties.entrySet()) {

            final Property value = e.getValue();

            if (value instanceof StringProperty) {
                result.put(e.getKey(), ((StringProperty) value).getValue());
            } else if (value instanceof IntegerProperty) {
                result.put(e.getKey(), ((IntegerProperty) value).getValue());
            } else if (value instanceof LongProperty) {
                result.put(e.getKey(), ((LongProperty) value).getValue());
            } else {
                CloudPublisherExporter.logger.info("discarding unsupported property {}", value);
            }

        }

        return result;
    }

}