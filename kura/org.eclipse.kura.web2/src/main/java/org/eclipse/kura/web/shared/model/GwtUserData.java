package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GwtUserData extends GwtBaseModel implements Serializable {

    @SuppressWarnings("unused")
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isAdmin() ? 1231 : 1237);
        result = prime * result + ((getPermissions() == null) ? 0 : getPermissions().hashCode());
        result = prime * result + ((getUserName() == null) ? 0 : getUserName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GwtUserData other = (GwtUserData) obj;
        if (isAdmin() != other.isAdmin())
            return false;
        if (getPermissions() == null) {
            if (other.getPermissions() != null)
                return false;
        } else if (!getPermissions().equals(other.getPermissions()))
            return false;
        if (getUserName() == null) {
            if (other.getUserName() != null)
                return false;
        } else if (!getUserName().equals(other.getUserName()))
            return false;
        return true;
    }

}
