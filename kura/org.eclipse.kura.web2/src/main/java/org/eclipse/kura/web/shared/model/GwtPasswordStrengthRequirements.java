/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtPasswordStrengthRequirements extends GwtBaseModel implements Serializable {

    private GwtUserData unused;

    private static final long serialVersionUID = 8697261888960678066L;

    public GwtPasswordStrengthRequirements() {
    }

    public GwtPasswordStrengthRequirements(final GwtPasswordStrengthRequirements other) {
        setPasswordMinimumLength(other.getPasswordMinimumLength());
        setPasswordRequireDigits(other.getPasswordRequireDigits());
        setPasswordRequireSpecialChars(other.getPasswordRequireSpecialChars());
        setPasswordRequireBothCases(other.getPasswordRequireBothCases());
    }

    public void setPasswordMinimumLength(final int minimumPasswordLength) {
        set("minimumPasswordLength", minimumPasswordLength);
    }

    public int getPasswordMinimumLength() {
        return (Integer) get("minimumPasswordLength");
    }

    public void setPasswordRequireDigits(final boolean passwordRequireDigits) {
        set("passwordRequireDigits", passwordRequireDigits);
    }

    public boolean getPasswordRequireDigits() {
        return (Boolean) get("passwordRequireDigits");
    }

    public void setPasswordRequireSpecialChars(final boolean passwordRequireSpecialChars) {
        set("passwordRequireSpecialChars", passwordRequireSpecialChars);
    }

    public boolean getPasswordRequireSpecialChars() {
        return (Boolean) get("passwordRequireSpecialChars");
    }

    public void setPasswordRequireBothCases(final boolean passwordRequireBothCases) {
        set("passwordRequireBothCases", passwordRequireBothCases);
    }

    public boolean getPasswordRequireBothCases() {
        return (Boolean) get("passwordRequireBothCases");
    }

    public void setUserData(final GwtUserData userData) {
        set("userData", userData);
    }

    public GwtUserData getUserData() {
        return get("userData");
    }

    public void allowAnyPassword() {
        setPasswordMinimumLength(0);
        setPasswordRequireDigits(false);
        setPasswordRequireSpecialChars(false);
        setPasswordRequireBothCases(false);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getPasswordMinimumLength();
        result = prime * result + (getPasswordRequireBothCases() ? 1231 : 1237);
        result = prime * result + (getPasswordRequireDigits() ? 1231 : 1237);
        result = prime * result + (getPasswordRequireSpecialChars() ? 1231 : 1237);
        result = prime * result + ((getUserData() == null) ? 0 : getUserData().hashCode());
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
        GwtPasswordStrengthRequirements other = (GwtPasswordStrengthRequirements) obj;
        if (getPasswordMinimumLength() != other.getPasswordMinimumLength())
            return false;
        if (getPasswordRequireBothCases() != other.getPasswordRequireBothCases())
            return false;
        if (getPasswordRequireDigits() != other.getPasswordRequireDigits())
            return false;
        if (getPasswordRequireSpecialChars() != other.getPasswordRequireSpecialChars())
            return false;
        if (getUserData() == null) {
            if (other.getUserData() != null)
                return false;
        } else if (!getUserData().equals(other.getUserData()))
            return false;
        return true;
    }

}
