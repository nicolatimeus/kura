package org.eclipse.kura.web.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class KuraPermission {

    public static final String STATUS = "kura.status";
    public static final String NETWORK_ADMIN = "kura.network.admin";
    public static final String PACKAGES_ADMIN = "kura.packages.admin";
    public static final String DEVICE = "kura.device";
    public static final String CLOUD_CONNECTION_ADMIN = "kura.connection.admin";
    public static final String SERVICES_ADMIN = "kura.services.admin";
    public static final String SETTINGS_ADMIN = "kura.settings.admin";
    public static final String USER_ADMIN = "kura.user.admin";
    public static final String SECURITY_ADMIN = "kura.security.admin";

    public static final Set<String> DEFAULT_PERMISSIONS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(STATUS, NETWORK_ADMIN, PACKAGES_ADMIN, DEVICE,
                    CLOUD_CONNECTION_ADMIN, SERVICES_ADMIN, SETTINGS_ADMIN, USER_ADMIN, SECURITY_ADMIN)));

    private KuraPermission() {
    }

}
