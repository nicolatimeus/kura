/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.s7plc;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.eclipse.kura.driver.s7plc.localization.S7PlcMessages;
import org.eclipse.kura.localization.LocalizationAdapter;

final class S7PlcOptions {

    private static final String IP = "host.ip";

    private static final String RACK = "rack";

    private static final String AUTHENTICATE = "authenticate";

    private static final String PASSWORD = "password";

    private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

    private static final String SLOT = "slot";

    private static final String TRANSFER_SIZE_HINT = "read.transfer.size.hint";

    private final Map<String, Object> properties;

    S7PlcOptions(final Map<String, Object> properties) {
        requireNonNull(properties, s_message.propertiesNonNull());
        this.properties = properties;
    }

    String getIp() {
        String ipAddress = null;
        if ((this.properties != null) && this.properties.containsKey(IP) && (this.properties.get(IP) != null)) {
            ipAddress = this.properties.get(IP).toString();
        }
        return ipAddress;
    }

    int getRack() {
        int rack = 0;
        if ((this.properties != null) && this.properties.containsKey(RACK) && (this.properties.get(RACK) != null)) {
            rack = Integer.valueOf(this.properties.get(RACK).toString());
        }
        return rack;
    }

    int getSlot() {
        int slot = 0;
        if ((this.properties != null) && this.properties.containsKey(SLOT) && (this.properties.get(SLOT) != null)) {
            slot = Integer.valueOf(this.properties.get(SLOT).toString());
        }
        return slot;
    }

    int getTransferSizeHint() {
        int transferSizeHint = 0;
        if ((this.properties != null) && this.properties.containsKey(TRANSFER_SIZE_HINT)
                && (this.properties.get(TRANSFER_SIZE_HINT) != null)) {
            transferSizeHint = Integer.valueOf(this.properties.get(TRANSFER_SIZE_HINT).toString());
        }
        return transferSizeHint;
    }

    boolean shouldAuthenticate() {
        boolean authenticate = false;
        if ((this.properties != null) && this.properties.containsKey(AUTHENTICATE)
                && (this.properties.get(AUTHENTICATE) != null)) {
            authenticate = Boolean.valueOf(this.properties.get(AUTHENTICATE).toString());
        }
        return authenticate;
    }

    String getPassword() {
        String password = "";
        if ((this.properties != null) && this.properties.containsKey(PASSWORD)
                && (this.properties.get(PASSWORD) != null)) {
            password = (String) this.properties.get(PASSWORD);
        }
        return password;
    }

}
