package org.eclipse.kura.internal.rest.provider;

import java.io.IOException;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(final ContainerRequestContext context) throws IOException {
        final RolesAllowed rolesAllowed = resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class);

        if (rolesAllowed != null) {
            final SecurityContext securityContext = context.getSecurityContext();

            if (securityContext == null) {
                context.abortWith(Response.status(Status.FORBIDDEN).build());
                return;
            }

            for (final String role : rolesAllowed.value()) {
                if (securityContext.isUserInRole(role)) {
                    return;
                }
            }

            context.abortWith(Response.status(Status.FORBIDDEN).build());
        }
    }

}
