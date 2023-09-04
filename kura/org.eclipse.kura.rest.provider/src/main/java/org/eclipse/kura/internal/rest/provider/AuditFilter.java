package org.eclipse.kura.internal.rest.provider;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.eclipse.kura.audit.AuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditFilter implements ContainerResponseFilter {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        int responseStatus = responseContext.getStatus();

        final AuditContext auditContext = RestServiceUtils.initAuditContext(requestContext, request);

        try {
            if (responseContext.getStatus() == 404) {
                auditLogger.warn("{} Rest - Failure - Service not found", auditContext);
                return;
            }

            if (responseContext.getStatus() == 403) {
                if (requestContext.getSecurityContext() == null
                        || requestContext.getSecurityContext().getUserPrincipal() == null) {
                    responseContext.setStatus(401);
                } else {
                    auditLogger.warn("{} Rest - Failure - User not authorized to perform the requested operation",
                            auditContext);
                    return;
                }

            }

            if (responseContext.getStatus() == 401) {
                auditLogger.warn("{} Rest - Failure - User not authenticated", auditContext);
                return;
            }

            if (responseStatus >= 200 && responseStatus < 400) {
                auditLogger.info("{} Rest - Success - Rest request succeeded", auditContext);
            } else {
                auditLogger.warn("{} Rest - Failure - Request failed", auditContext);
            }
        } finally {
            RestServiceUtils.closeAuditContext(requestContext);
        }
    }
}
