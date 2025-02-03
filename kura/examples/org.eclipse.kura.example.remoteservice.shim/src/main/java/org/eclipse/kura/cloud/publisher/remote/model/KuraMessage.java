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

@JsonTypeName("KuraMessage")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", //
        date = "2025-02-03T12:24:29.934778+01:00[Europe/Rome]", //
        comments = "Generator version: 7.11.0")
public class KuraMessage {

    private Map<String, Property> properties = new HashMap<>();
    private KuraPayload payload;

    public KuraMessage() {
    }

    /**
     **/
    public KuraMessage properties(Map<String, Property> properties) {
        this.properties = properties;
        return this;
    }

    @JsonProperty("properties")
    public Map<String, Property> getProperties() {
        return properties;
    }

    @JsonProperty("properties")
    public void setProperties(Map<String, Property> properties) {
        this.properties = properties;
    }

    public KuraMessage putPropertiesItem(String key, Property propertiesItem) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }

        this.properties.put(key, propertiesItem);
        return this;
    }

    public KuraMessage removePropertiesItem(String key) {
        if (this.properties != null) {
            this.properties.remove(key);
        }

        return this;
    }

    /**
     **/
    public KuraMessage payload(KuraPayload payload) {
        this.payload = payload;
        return this;
    }

    @JsonProperty("payload")
    public KuraPayload getPayload() {
        return payload;
    }

    @JsonProperty("payload")
    public void setPayload(KuraPayload payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KuraMessage kuraMessage = (KuraMessage) o;
        return Objects.equals(this.properties, kuraMessage.properties)
                && Objects.equals(this.payload, kuraMessage.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, payload);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class KuraMessage {\n");

        sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
        sb.append("    payload: ").append(toIndentedString(payload)).append("\n");
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
