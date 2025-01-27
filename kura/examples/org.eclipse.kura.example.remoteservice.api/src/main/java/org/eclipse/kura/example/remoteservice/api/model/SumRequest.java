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
package org.eclipse.kura.example.remoteservice.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.annotation.Generated;

@JsonTypeName("SumRequest")
@Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", //
        date = "2025-01-24T12:12:34.933658+01:00[Europe/Rome]", //
        comments = "Generator version: 7.12.0-SNAPSHOT")
public class SumRequest {

    private Integer a;
    private Integer b;

    public SumRequest() {
    }

    @JsonCreator
    public SumRequest(@JsonProperty(required = true, value = "a") Integer a,
            @JsonProperty(required = true, value = "b") Integer b) {
        this.a = a;
        this.b = b;
    }

    /**
     **/
    public SumRequest a(Integer a) {
        this.a = a;
        return this;
    }

    @JsonProperty(required = true, value = "a")
    public Integer getA() {
        return a;
    }

    @JsonProperty(required = true, value = "a")
    public void setA(Integer a) {
        this.a = a;
    }

    /**
     **/
    public SumRequest b(Integer b) {
        this.b = b;
        return this;
    }

    @JsonProperty(required = true, value = "b")
    public Integer getB() {
        return b;
    }

    @JsonProperty(required = true, value = "b")
    public void setB(Integer b) {
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SumRequest sumRequest = (SumRequest) o;
        return Objects.equals(this.a, sumRequest.a) && Objects.equals(this.b, sumRequest.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SumRequest {\n");

        sb.append("    a: ").append(toIndentedString(a)).append("\n");
        sb.append("    b: ").append(toIndentedString(b)).append("\n");
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
