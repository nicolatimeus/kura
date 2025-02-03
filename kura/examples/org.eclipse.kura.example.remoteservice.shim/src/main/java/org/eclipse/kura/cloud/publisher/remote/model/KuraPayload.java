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
package org.eclipse.kura.cloud.publisher.remote.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("KuraPayload")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", //
        date = "2025-02-03T12:24:29.934778+01:00[Europe/Rome]", //
        comments = "Generator version: 7.11.0")
public class KuraPayload {

    private Map<String, Property> metrics = new HashMap<>();

    public KuraPayload() {
    }

    /**
     **/
    public KuraPayload metrics(Map<String, Property> metrics) {
        this.metrics = metrics;
        return this;
    }

    @JsonProperty("metrics")
    public Map<String, Property> getMetrics() {
        return metrics;
    }

    @JsonProperty("metrics")
    public void setMetrics(Map<String, Property> metrics) {
        this.metrics = metrics;
    }

    public KuraPayload putMetricsItem(String key, Property metricsItem) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }

        this.metrics.put(key, metricsItem);
        return this;
    }

    public KuraPayload removeMetricsItem(String key) {
        if (this.metrics != null) {
            this.metrics.remove(key);
        }

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KuraPayload kuraPayload = (KuraPayload) o;
        return Objects.equals(this.metrics, kuraPayload.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metrics);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class KuraPayload {\n");

        sb.append("    metrics: ").append(toIndentedString(metrics)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
