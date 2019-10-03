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

public class GwtUserInfo extends GwtBaseModel implements Serializable {

    private GwtUserData unused;

    private static final long serialVersionUID = 8697261888960678066L;

    public GwtUserInfo() {
    }

    public GwtUserInfo(final GwtUserInfo other) {
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

}
