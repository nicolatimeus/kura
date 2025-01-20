package org.eclipse.kura.remoteservice.discovery.fs.provider;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class Utils {

    private Utils() {
    }

    public static String getId(final EndpointDescription description) {
        return description.getFrameworkUUID() + "-" + description.getServiceId();
    }
}
