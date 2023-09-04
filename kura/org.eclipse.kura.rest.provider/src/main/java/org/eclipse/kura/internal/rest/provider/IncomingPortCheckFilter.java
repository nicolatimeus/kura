package org.eclipse.kura.internal.rest.provider;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
class IncomingPortCheckFilter implements ContainerRequestFilter {

    private static final Response NOT_FOUND_RESPONSE = Response.status(Response.Status.NOT_FOUND).build();

    @Context
    private HttpServletRequest request;

    private Set<Integer> allowedPorts = Collections.emptySet();

    public void setAllowedPorts(final Set<Integer> allowedPorts) {
        this.allowedPorts = allowedPorts;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        RestServiceUtils.initAuditContext(requestContext, request);

        if (allowedPorts.isEmpty()) {
            return;
        }

        final int port = request.getLocalPort();

        if (!allowedPorts.contains(port)) {
            requestContext.abortWith(NOT_FOUND_RESPONSE);
        }
    }

}