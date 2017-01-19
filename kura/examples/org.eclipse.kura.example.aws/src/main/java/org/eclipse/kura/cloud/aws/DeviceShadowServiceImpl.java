/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud.aws;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class DeviceShadowServiceImpl implements DeviceShadowService, DataServiceListener {

    private static final Logger s_logger = LoggerFactory.getLogger(DeviceShadowServiceImpl.class);

    private static final String DEVICE_SHADOW_TOPICS_PREFIX = "$aws/things/";
    private static final String DEVICE_SHADOW_GET_SUFFIX = "/shadow/get";
    private static final String DEVICE_SHADOW_GET_ACCEPTED_SUFFIX = "/shadow/get/accepted";
    private static final String DEVICE_SHADOW_GET_REJECTED_SUFFIX = "/shadow/get/rejected";
    private static final String DEVICE_SHADOW_UPDATE_DOCUMENTS_SUFFIX = "/shadow/update/documents";
    private static final String DEVICE_SHADOW_UPDATE_SUFFIX = "/shadow/update";
    private static final String DEVICE_SHADOW_UPDATE_REJECTED_SUFFIX = "/shadow/update/rejected";

    private String deviceShadowGetTopic;
    private String deviceShadowGetAcceptedTopic;
    private String deviceShadowGetRejectedTopic;
    private String deviceShadowUpdateDocumentsTopic;
    private String deviceShadowUpdateTopic;
    private String deviceShadowUpdateRejectedTopic;

    private DataService dataService;
    private JsonValue shadow;
    private long shadowVersion = -1;

    private DataTransportService dataTransportService;

    private ServiceTracker<DeviceShadowListener, DeviceShadowListener> listenerTracker;

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public void unsetDataService(DataService dataService) {
        this.dataService = null;
    }

    public void setDataTransportService(DataTransportService dataTransportService) {
        this.dataTransportService = dataTransportService;
    }

    public void unsetDataTransportService(DataTransportService dataTransportService) {
        this.dataTransportService = null;
    }

    public void activate() {
        s_logger.info("activating..");

        try {
            String clientId = dataTransportService.getClientId();

            if (clientId == null) {
                throw new IllegalStateException(
                        "Client ID is null, please set the client for the DataTransportService");
            }

            this.deviceShadowGetTopic = DEVICE_SHADOW_TOPICS_PREFIX + clientId + DEVICE_SHADOW_GET_SUFFIX;
            this.deviceShadowGetAcceptedTopic = DEVICE_SHADOW_TOPICS_PREFIX + clientId
                    + DEVICE_SHADOW_GET_ACCEPTED_SUFFIX;
            this.deviceShadowUpdateDocumentsTopic = DEVICE_SHADOW_TOPICS_PREFIX + clientId
                    + DEVICE_SHADOW_UPDATE_DOCUMENTS_SUFFIX;
            this.deviceShadowUpdateTopic = DEVICE_SHADOW_TOPICS_PREFIX + clientId + DEVICE_SHADOW_UPDATE_SUFFIX;
            this.deviceShadowGetRejectedTopic = DEVICE_SHADOW_TOPICS_PREFIX + clientId
                    + DEVICE_SHADOW_GET_REJECTED_SUFFIX;
            this.deviceShadowUpdateRejectedTopic = DEVICE_SHADOW_TOPICS_PREFIX + clientId
                    + DEVICE_SHADOW_UPDATE_REJECTED_SUFFIX;

            final BundleContext context = FrameworkUtil.getBundle(DeviceShadowServiceImpl.class).getBundleContext();

            listenerTracker = new ServiceTracker<DeviceShadowListener, DeviceShadowListener>(context,
                    DeviceShadowListener.class, new ListenerTrackerCustomizer(context));

            for (ServiceReference<DeviceShadowListener> ref : context.getServiceReferences(DeviceShadowListener.class,
                    null)) {
                listenerTracker.addingService(ref);
            }

            listenerTracker.open();

            dataService.addDataServiceListener(this);
            if (dataService.isConnected()) {
                onConnectionEstablished();
            }
        } catch (Exception e) {
            throw new ComponentException(e);
        }
    }

    public void deactivate() {
        s_logger.info("deactivating..");
        dataService.removeDataServiceListener(this);

        listenerTracker.close();
        listenerTracker = null;
    }

    private void fetchDeviceShadow() {
        try {
            this.dataService.publish(this.deviceShadowGetTopic, new byte[0], 0, false, 0);
        } catch (KuraStoreException e) {
            s_logger.warn("failed to request device shadow", e);
        }
    }

    private void notifyShadowChange(JsonValue shadow) {
        Object[] services = listenerTracker.getServices();

        if (services == null) {
            return;
        }

        for (Object o : services) {
            DeviceShadowListener listener = (DeviceShadowListener) o;
            listener.onShadowChanged(shadow);
        }
    }

    private void notifyShadowUpdateFailure(JsonValue message) {
        Object[] services = listenerTracker.getServices();

        if (services == null) {
            return;
        }

        for (Object o : services) {
            DeviceShadowListener listener = (DeviceShadowListener) o;
            listener.onShadowUpdateFailed(message);
        }
    }

    private void notifyShadowGetFailure(JsonValue message) {
        Object[] services = listenerTracker.getServices();

        if (services == null) {
            return;
        }

        for (Object o : services) {
            DeviceShadowListener listener = (DeviceShadowListener) o;
            listener.onShadowGetFailed(message);
        }
    }

    @Override
    public JsonValue getShadow() {
        return shadow;
    }

    @Override
    public void updateReportedShadow(JsonObject reportedShadow) throws KuraException {
        JsonObject root = Json.object();
        JsonObject state = Json.object();

        root.set("state", state);
        if (this.shadowVersion != -1) {
            root.set("version", this.shadowVersion);
        }
        state.set("reported", reportedShadow);

        dataService.publish(this.deviceShadowUpdateTopic, root.toString().getBytes(), 0, false, 0);
    }

    @Override
    public void onConnectionEstablished() {
        s_logger.info("Connection enstabilished, fetching shadow");
        try {
            dataService.subscribe(this.deviceShadowGetAcceptedTopic, 0);
            dataService.subscribe(this.deviceShadowUpdateDocumentsTopic, 0);
            dataService.subscribe(this.deviceShadowGetRejectedTopic, 0);
            dataService.subscribe(this.deviceShadowUpdateRejectedTopic, 0);
        } catch (Exception e) {
            s_logger.warn("failed to subscribe to shadow related topics", e);
        }
        fetchDeviceShadow();
    }

    @Override
    public void onDisconnecting() {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionLost(Throwable cause) {
    }

    private void extractShadow(JsonValue response) {
        this.shadowVersion = response.asObject().get("version").asLong();
        this.shadow = response.asObject().get("state");
    }

    private void extractShadowFromUpdate(JsonValue response) {
        extractShadow(response.asObject().get("current"));
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        try {
            if (this.deviceShadowUpdateRejectedTopic.equals(topic)) {
                JsonValue message = Json.parse(new String(payload));
                notifyShadowUpdateFailure(message);
                int code = message.asObject().getInt("code", -1);
                if (code == 409) {
                    s_logger.warn("Shadow version conflict reported, re-fetching shadow");
                    s_logger.warn("Last known shadow version: {}", this.shadowVersion);
                    fetchDeviceShadow();
                }
                return;
            } else if (this.deviceShadowGetRejectedTopic.equals(topic)) {
                notifyShadowGetFailure(Json.parse(new String(payload)));
                return;
            }

            boolean isGetResponse = deviceShadowGetAcceptedTopic.equals(topic);
            boolean isUpdate = deviceShadowUpdateDocumentsTopic.equals(topic);

            if (!(isGetResponse || isUpdate)) {
                return;
            }

            JsonValue receivedData = Json.parse(new String(payload));

            if (isGetResponse) {
                extractShadow(receivedData);
            } else {
                extractShadowFromUpdate(receivedData);
            }

            s_logger.info("Shadow version: " + shadowVersion);
            notifyShadowChange(shadow);
        } catch (Exception e) {
            s_logger.warn("Failed to handle received message", e);
        }
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
    }

    private final class ListenerTrackerCustomizer
            implements ServiceTrackerCustomizer<DeviceShadowListener, DeviceShadowListener> {

        private final BundleContext context;

        private ListenerTrackerCustomizer(BundleContext context) {
            this.context = context;
        }

        @Override
        public DeviceShadowListener addingService(ServiceReference<DeviceShadowListener> reference) {
            DeviceShadowListener listener = context.getService(reference);
            if (shadow != null) {
                listener.onShadowChanged(shadow);
            } else {
                fetchDeviceShadow();
            }
            return listener;
        }

        @Override
        public void modifiedService(ServiceReference<DeviceShadowListener> reference, DeviceShadowListener service) {
        }

        @Override
        public void removedService(ServiceReference<DeviceShadowListener> reference, DeviceShadowListener service) {
        }
    }
}
