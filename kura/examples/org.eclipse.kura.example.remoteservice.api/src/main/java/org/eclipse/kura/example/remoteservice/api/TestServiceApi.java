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
package org.eclipse.kura.example.remoteservice.api;

import org.eclipse.kura.example.remoteservice.api.model.SumRequest;

import jakarta.annotation.Generated;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * Represents a collection of functions to interact with the API endpoints.
 */
@Path("")
@Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", //
        date = "2025-01-24T12:27:15.391007+01:00[Europe/Rome]",  //
        comments = "Generator version: 7.12.0-SNAPSHOT")
public interface TestServiceApi {

    /**
     * 
     *
     * @return Returns a constant string
     */
    @GET
    @Path("/ping")
    @Produces({ "application/json" })
    String ping();

    /**
     * 
     *
     * @param sumRequest
     *            Performs the sum of two integers
     * @return The sum result
     */
    @POST
    @Path("/sum")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    Integer sum(SumRequest sumRequest);

}
