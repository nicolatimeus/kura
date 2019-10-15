/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.server.util.ServiceLocator;

public class AuthenticationManager {

    private static AuthenticationManager instance;

    public static AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }

    public boolean authenticate(String username, String password) throws KuraException {

        try {
            final Optional<String> registeredPassword = Console.getConsoleOptions().getUserConfiguration()
                    .getUserPassword(username);

            if (!registeredPassword.isPresent()) {
                throw new IllegalArgumentException();
            }

            CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
            String sha1Password = cryptoService.sha1Hash(password);

            return Arrays.equals(sha1Password.toCharArray(), registeredPassword.get().toCharArray());
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }
}
