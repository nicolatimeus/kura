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
package org.eclipse.kura.cloud.publisher.remote;

import org.eclipse.kura.cloud.publisher.remote.model.KuraMessage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * Represents a collection of functions to interact with the API endpoints.
 */

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", //
        date = "2025-02-03T09:07:42.954677+01:00[Europe/Rome]",  //
        comments = "Generator version: 7.11.0")
public interface PublisherApi {

    /**
     * 
     *
     * @param kuraMessage
     * @return Success
     */
    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Path("/publisher/publish")
    String publish(KuraMessage kuraMessage);

}
