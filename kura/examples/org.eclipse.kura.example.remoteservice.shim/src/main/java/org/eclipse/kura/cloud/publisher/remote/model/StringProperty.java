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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("StringProperty")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", //
        date = "2025-02-03T12:24:29.934778+01:00[Europe/Rome]", //
        comments = "Generator version: 7.11.0")
public class StringProperty extends Property {

    private String value;

    public StringProperty() {
    }

    @JsonCreator
    public StringProperty(@JsonProperty(required = true, value = "value") String value,
            @JsonProperty(required = true, value = "type") TypeEnum type) {
        super(type);
        this.value = value;
    }

    /**
     **/
    public StringProperty value(String value) {
        this.value = value;
        return this;
    }

    @JsonProperty(required = true, value = "value")
    public String getValue() {
        return value;
    }

    @JsonProperty(required = true, value = "value")
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringProperty stringProperty = (StringProperty) o;
        return Objects.equals(this.value, stringProperty.value) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StringProperty {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
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
