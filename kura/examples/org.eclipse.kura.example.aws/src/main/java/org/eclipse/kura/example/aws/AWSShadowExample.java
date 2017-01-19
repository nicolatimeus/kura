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

import org.eclipse.kura.cloud.aws.DeviceShadowListener;
import org.eclipse.kura.cloud.aws.DeviceShadowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class AWSShadowExample implements DeviceShadowListener {

    private static final Logger s_logger = LoggerFactory.getLogger(AWSShadowExample.class);

    private DeviceShadowService deviceShadowService;

    public void setDeviceShadowService(DeviceShadowService deviceShadowService) {
        this.deviceShadowService = deviceShadowService;
    }

    public void unsetDeviceShadowService(DeviceShadowService deviceShadowService) {
        this.deviceShadowService = null;
    }

    public void activate() {
        s_logger.info("activating...");
    }

    public void deactivate() {
        s_logger.info("deactivating...");
    }

    private JsonValue getPropertySafe(JsonValue o, String... path) {
        JsonValue current = o;
        for (int i = 0; i < path.length; i++) {
            if (current == null || !current.isObject()) {
                return null;
            }
            current = current.asObject().get(path[i]);
        }
        return current;
    }

    @Override
    public void onShadowChanged(JsonValue shadow) {
        s_logger.info("Shadow changed: {}", shadow);

        JsonValue desiredEcho = getPropertySafe(shadow, "desired", "echo");
        JsonValue reportedEcho = getPropertySafe(shadow, "reported", "echo");

        if (desiredEcho == null || desiredEcho.equals(reportedEcho)) {
            return;
        }

        JsonObject reportedShadow = Json.object();
        reportedShadow.set("echo", desiredEcho);

        try {
            s_logger.info("updating property echo of reported shadow");
            deviceShadowService.updateReportedShadow(reportedShadow);
        } catch (Exception e) {
            s_logger.warn("failed to send shadow update request", e);
        }

    }

    @Override
    public void onShadowUpdateFailed(JsonValue message) {
        s_logger.warn("shadow update failed: " + message);
    }

    @Override
    public void onShadowGetFailed(JsonValue error) {
        if (error.asObject().getInt("code", -1) == 404) {
            s_logger.info("shadow is empty, creating new one");
            JsonObject reportedShadow = Json.object();
            reportedShadow.set("echo", "modify this property in the desired shadow");

            try {
                deviceShadowService.updateReportedShadow(reportedShadow);
            } catch (Exception e) {
                s_logger.warn("failed to send shadow update request", e);
            }
        }
    }

}
