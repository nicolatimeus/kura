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
package org.eclipse.kura.example.aws;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.message.json.KuraPayloadJsonMarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AWSPublisher implements ConfigurableComponent, CloudClientListener {

    private static final Logger s_logger = LoggerFactory.getLogger(AWSPublisher.class);

    // Publishing Property Names
    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    private static final String TEMP_INITIAL_PROP_NAME = "metric.temperature.initial";
    private static final String TEMP_INCREMENT_PROP_NAME = "metric.temperature.increment";
    private static final String[] METRIC_PROP_NAMES = { "metric.string", "metric.string.oneof", "metric.long",
            "metric.integer", "metric.integer.fixed", "metric.short", "metric.double", "metric.float", "metric.char",
            "metric.byte", "metric.boolean", "metric.password" };

    // Application topics

    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.topic";
    private static final String SUBSCRIBE_TOPIC_PROP_NAME = "subscribe.topic";
    private static final String APP_NAME_PROP_NAME = "app.name";

    private CloudService cloudService;
    private CloudClient cloudClient;

    private final ScheduledExecutorService m_worker;
    private ScheduledFuture<?> m_handle;

    private float m_temperature;
    private String m_publishTopic;
    private String m_subscribeTopic;
    private String m_appName;

    private Map<String, Object> m_properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public AWSPublisher() {
        super();
        this.m_worker = Executors.newSingleThreadScheduledExecutor();
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating AWSPublisher...");

        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Activate - " + s + ": " + properties.get(s));
        }

        try {
            doUpdate();
        } catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        s_logger.info("Activating AWSPublisher... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("Deactivating AWSPublisher...");

        // shutting down the worker and cleaning up the properties
        this.m_worker.shutdown();

        releaseCloudClient();

        this.m_appName = null;
        this.m_publishTopic = null;
        this.m_subscribeTopic = null;

        s_logger.debug("Deactivating AWSPublisher... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated AWSPublisher...");

        // store the properties received
        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        try {
            doUpdate();
            s_logger.info("Updated AWSPublisher... Done.");
        } catch (Exception e) {
            s_logger.info("Failed to update compent", e);
        }
    }

    // ----------------------------------------------------------------
    //
    // DataServiceListener Callback Methods
    //
    // ----------------------------------------------------------------

    @Override
    public void onConnectionEstablished() {
        s_logger.info("Connection established");
        try {
            s_logger.info("Subscribing to application topic {}", this.m_subscribeTopic);
            this.cloudClient.subscribe(this.m_subscribeTopic, 0);
        } catch (KuraStoreException e) {
            s_logger.warn("Failed to request device shadow", e);
        } catch (KuraException e) {
            s_logger.warn("Failed to subscribe", e);
        }
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        s_logger.info("Published message with ID: {} on application topic: {}", messageId, appTopic);
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        s_logger.info("Confirmed message with ID: {} on application topic: {}", messageId, appTopic);
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.info("Control Message Arrived - deviceId: " + deviceId);
        s_logger.info("Control Message Arrived - appTopic: " + appTopic);
        s_logger.info("Control Message Arrived - payload: " + new String(msg.getBody()));
        s_logger.info("Control Message Arrived - qos: " + qos);
        s_logger.info("Control Message Arrived - retained: " + retain);
        s_logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        s_logger.info("Control Message Arrived - deviceId: " + deviceId);
        s_logger.info("Control Message Arrived - appTopic: " + appTopic);
        s_logger.info("Message Arrived - payload: " + new String(msg.getBody()));
        s_logger.info("Message Arrived - qos: " + qos);
        s_logger.info("Message Arrived - retained: " + retain);
    }

    @Override
    public void onConnectionLost() {
        s_logger.warn("Connection lost!");
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     * 
     * @throws KuraException
     */
    private void doUpdate() throws KuraException {
        // cancel a current worker handle if one if active
        if (this.m_handle != null) {
            this.m_handle.cancel(true);
        }

        if (!this.m_properties.containsKey(TEMP_INITIAL_PROP_NAME)
                || !this.m_properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
            s_logger.info(
                    "Update AWSPublisher - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
            return;
        }

        // reset the temperature to the initial value
        this.m_temperature = (Float) this.m_properties.get(TEMP_INITIAL_PROP_NAME);

        // set the application name from the properties
        String newAppName = (String) this.m_properties.get(APP_NAME_PROP_NAME);

        // set the publish topic from the properties
        this.m_publishTopic = (String) this.m_properties.get(PUBLISH_TOPIC_PROP_NAME);

        // set the subscribe topic from the properties
        this.m_subscribeTopic = (String) this.m_properties.get(SUBSCRIBE_TOPIC_PROP_NAME);

        if (!newAppName.equals(this.m_appName)) {
            this.m_appName = newAppName;
            releaseCloudClient();
            acquireCloudClient();
        }

        // schedule a new worker based on the properties of the service
        int pubrate = (Integer) this.m_properties.get(PUBLISH_RATE_PROP_NAME);
        this.m_handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                doPublish();
            }
        }, 0, pubrate, TimeUnit.MILLISECONDS);
    }

    private void acquireCloudClient() throws KuraException {
        // Acquire a Cloud Application Client for this Application
        s_logger.info("Getting CloudApplicationClient for {}...", this.m_appName);
        this.cloudClient = this.cloudService.newCloudClient(this.m_appName);
        if (this.cloudClient.isConnected()) {
            this.onConnectionEstablished();
        }
        this.cloudClient.addCloudClientListener(this);
    }

    private void releaseCloudClient() {
        // Releasing the CloudApplicationClient
        if (this.cloudClient != null) {
            s_logger.info("Releasing CloudApplicationClient for {}...", this.m_appName);
            this.cloudClient.release();
        }
    }

    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() {
        // Increment the simulated temperature value
        float tempIncr = (Float) this.m_properties.get(TEMP_INCREMENT_PROP_NAME);
        this.m_temperature += tempIncr;

        // Add the temperature as a metric to the payload
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());
        payload.addMetric("temperature", this.m_temperature);

        // add all the other metrics
        for (String metric : METRIC_PROP_NAMES) {
            payload.addMetric(metric, this.m_properties.get(metric));
        }

        // Publish the message
        // fetch the publishing configuration from the publishing properties
        Integer qos = (Integer) this.m_properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) this.m_properties.get(PUBLISH_RETAIN_PROP_NAME);
        Integer priority = 5; // as recommended by Kura guidelines

        try {
            int messageId = this.cloudClient.publish(this.m_publishTopic,
                    KuraPayloadJsonMarshaller.toJson(payload).toString().getBytes(), qos, retain, priority);
            s_logger.info("Published to {} message: {} with ID: {}",
                    new Object[] { this.m_publishTopic, payload, messageId });
        } catch (Exception e) {
            s_logger.error("Cannot publish topic: " + this.m_publishTopic, e);
        }
    }
}
