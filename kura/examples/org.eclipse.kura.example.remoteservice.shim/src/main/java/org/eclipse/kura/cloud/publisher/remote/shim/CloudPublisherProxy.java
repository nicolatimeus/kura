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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.publisher.remote.PublisherApi;
import org.eclipse.kura.cloud.publisher.remote.model.IntegerProperty;
import org.eclipse.kura.cloud.publisher.remote.model.KuraPayload;
import org.eclipse.kura.cloud.publisher.remote.model.LongProperty;
import org.eclipse.kura.cloud.publisher.remote.model.Property;
import org.eclipse.kura.cloud.publisher.remote.model.StringProperty;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudPublisherProxy implements CloudPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CloudPublisherProxy.class);

    private final PublisherApi remote;

    public CloudPublisherProxy(final PublisherApi remote) {
        this.remote = remote;
    }

    private static final Map<String, Property> exportProperties(final Map<String, Object> properties) {
        final Map<String, Property> result = new HashMap<>();

        if (properties == null) {
            return result;
        }

        for (final Entry<String, Object> e : properties.entrySet()) {
            final Object value = e.getValue();

            if (value instanceof String) {
                final StringProperty property = new StringProperty();
                property.setValue((String) value);

                result.put(e.getKey(), property);
            } else if (value instanceof Integer) {
                final IntegerProperty property = new IntegerProperty();
                property.setValue((Integer) value);

                result.put(e.getKey(), property);

            } else if (value instanceof Long) {
                final LongProperty property = new LongProperty();
                property.setValue((Long) value);

                result.put(e.getKey(), property);

            } else {
                CloudPublisherImporter.logger.warn("ignoring unsupported property: {}", e.getKey());
            }
        }

        return result;
    }

    @Override
    public String publish(final KuraMessage message) throws KuraException {

        final Map<String, Property> messageProperties = exportProperties(message.getProperties());
        final Map<String, Property> payloadProperties = exportProperties(message.getPayload().metrics());

        final org.eclipse.kura.cloud.publisher.remote.model.KuraMessage kuraMessage;
        kuraMessage = new org.eclipse.kura.cloud.publisher.remote.model.KuraMessage();
        final KuraPayload payload = new KuraPayload();
        payload.setMetrics(payloadProperties);
        kuraMessage.setPayload(payload);
        kuraMessage.setProperties(messageProperties);

        try {
            return this.remote.publish(kuraMessage);
        } catch (final Exception e) {
            logger.warn("Failed to publish", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e.getCause());
        }
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        CloudPublisherImporter.logger.warn("CloudPublisherProxy does not support listeners");
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        CloudPublisherImporter.logger.warn("CloudPublisherProxy does not support listeners");

    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        CloudPublisherImporter.logger.warn("CloudPublisherProxy does not support listeners");

    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        CloudPublisherImporter.logger.warn("CloudPublisherProxy does not support listeners");

    }

}