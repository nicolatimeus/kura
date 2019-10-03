package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GwtUserData extends GwtBaseModel implements Serializable {

    private HashSet<String> unused;

    /**
     * 
     */
    private static final long serialVersionUID = -1334340006399833329L;

    public GwtUserData() {
    }

    public GwtUserData(final String userName, final Set<String> permissions, final boolean isAdmin) {
        setUserName(userName);
        setPermissions(permissions);
        setIsAdmin(isAdmin);
    }

    public void setUserName(final String userName) {
        set("userName", userName);
    }

    public void setPermissions(final Set<String> permissions) {
        set("permissions", new HashSet<>(permissions));
    }

    public void setIsAdmin(final boolean isAdmin) {
        set("isAdmin", isAdmin);
    }

    public String getUserName() {
        return get("userName");
    }

    public Set<String> getPermissions() {
        return get("permissions");
    }

    public boolean isAdmin() {
        return get("isAdmin");
    }

}
