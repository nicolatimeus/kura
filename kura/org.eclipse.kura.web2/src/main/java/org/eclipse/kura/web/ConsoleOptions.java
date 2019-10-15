/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.web;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.configuration.Property;
import org.eclipse.kura.web.shared.model.GwtPasswordStrengthRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleOptions {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleOptions.class);

    static final String PROP_CONSOLE_USERNAME = "console.username.value";
    static final String PROP_CONSOLE_PASSWORD = "console.password.value";
    static final String PROP_APP_ROOT = "app.root";
    static final String PROP_SESSION_MAX_INACTIVITY_INTERVAL = "session.max.inactivity.interval";
    static final String PROP_ACCESS_BANNER_ENABLED = "access.banner.enabled";
    static final String PROP_ACCESS_BANNER_CONTENT = "access.banner.content";

    static final String PROP_PW_MIN_LENGTH = "new.password.min.length";
    static final String PROP_PW_REQUIRE_DIGITS = "new.password.require.digits";
    static final String PROP_PW_REQUIRE_SPECIAL_CHARS = "new.password.require.special.characters";
    static final String PROP_PW_REQUIRE_BOTH_CASES = "new.password.require.both.cases";

    static final String PROP_USER_CONFIGURATION = "user.configuration";

    private static final Property<String> CONSOLE_USERNAME = new Property<>(PROP_CONSOLE_USERNAME, "admin");
    private static final Property<String> CONSOLE_PASSWORD = new Property<>(PROP_CONSOLE_PASSWORD, "admin");
    private static final Property<String> CONSOLE_APP_ROOT = new Property<>(PROP_APP_ROOT, "/admin/console");
    private static final Property<Integer> SESSION_MAX_INACTIVITY_INTERVAL = new Property<>(
            PROP_SESSION_MAX_INACTIVITY_INTERVAL, 15);
    private static final Property<Boolean> ACCESS_BANNER_ENABLED = new Property<>(PROP_ACCESS_BANNER_ENABLED, false);
    private static final Property<String> ACCESS_BANNER_CONTENT = new Property<>(PROP_ACCESS_BANNER_CONTENT, "");

    private static final Property<Integer> PW_MIN_LENGTH = new Property<>(PROP_PW_MIN_LENGTH, 0);
    private static final Property<Boolean> PW_REQUIRE_DIGITS = new Property<>(PROP_PW_REQUIRE_DIGITS, false);
    private static final Property<Boolean> PW_REQUIRE_SPECIAL_CHARS = new Property<>(PROP_PW_REQUIRE_SPECIAL_CHARS,
            false);
    private static final Property<Boolean> PW_REQUIRE_BOTH_CASES = new Property<>(PROP_PW_REQUIRE_BOTH_CASES, false);

    private static final Property<String> USER_CONFIGURATION = new Property<>(PROP_USER_CONFIGURATION,
            "{\"permissions\": {}, \"passwords\": {}}");

    private final String appRoot;
    private final int sessionMaxInactivityInterval;
    private final boolean bannerEnabled;
    private final String bannerContent;
    private final GwtPasswordStrengthRequirements passwordStrengthRequirements;
    private final UserConfiguration userConfiguration;

    public ConsoleOptions(Map<String, Object> properties, final CryptoService cryptoService)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, KuraException {
        final String adminUsername = CONSOLE_USERNAME.get(properties);
        final String adminPassword = loadAdminPassword(cryptoService, (String) CONSOLE_PASSWORD.get(properties));

        this.appRoot = CONSOLE_APP_ROOT.get(properties);
        this.sessionMaxInactivityInterval = SESSION_MAX_INACTIVITY_INTERVAL.get(properties);
        this.bannerEnabled = ACCESS_BANNER_ENABLED.get(properties);
        this.bannerContent = ACCESS_BANNER_CONTENT.get(properties);
        this.passwordStrengthRequirements = extractPasswordStrengthRequirements(properties);

        UserConfiguration userConfig;

        try {
            userConfig = new UserConfiguration(cryptoService, adminUsername, adminPassword,
                    USER_CONFIGURATION.get(properties));
        } catch (final Exception e) {
            logger.warn("failed to deserialize users configuration, only admin user will be available", e);
            userConfig = new UserConfiguration(cryptoService, adminUsername, adminPassword);
        }

        this.userConfiguration = userConfig;
    }

    public String getAppRoot() {
        return this.appRoot;
    }

    public int getSessionMaxInactivityInterval() {
        return this.sessionMaxInactivityInterval;
    }

    public boolean isBannerEnabled() {
        return this.bannerEnabled;
    }

    public String getBannerContent() {
        return this.bannerContent;
    }

    public UserConfiguration getUserConfiguration() {
        return userConfiguration;
    }

    public GwtPasswordStrengthRequirements getUserOptions() {
        return new GwtPasswordStrengthRequirements(this.passwordStrengthRequirements);
    }

    private static String loadAdminPassword(final CryptoService cryptoService, final String password)
            throws KuraException, NoSuchAlgorithmException, UnsupportedEncodingException {
        final char[] decrypted = cryptoService.decryptAes(password.toCharArray());
        return cryptoService.sha1Hash(new String(decrypted));
    }

    private static GwtPasswordStrengthRequirements extractPasswordStrengthRequirements(
            final Map<String, Object> properties) {
        final GwtPasswordStrengthRequirements result = new GwtPasswordStrengthRequirements();

        result.setPasswordMinimumLength(PW_MIN_LENGTH.get(properties));
        result.setPasswordRequireDigits(PW_REQUIRE_DIGITS.get(properties));
        result.setPasswordRequireSpecialChars(PW_REQUIRE_SPECIAL_CHARS.get(properties));
        result.setPasswordRequireBothCases(PW_REQUIRE_BOTH_CASES.get(properties));

        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appRoot == null) ? 0 : appRoot.hashCode());
        result = prime * result + ((bannerContent == null) ? 0 : bannerContent.hashCode());
        result = prime * result + (bannerEnabled ? 1231 : 1237);
        result = prime * result
                + ((passwordStrengthRequirements == null) ? 0 : passwordStrengthRequirements.hashCode());
        result = prime * result + sessionMaxInactivityInterval;
        result = prime * result + ((userConfiguration == null) ? 0 : userConfiguration.hashCode());
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
        ConsoleOptions other = (ConsoleOptions) obj;
        if (appRoot == null) {
            if (other.appRoot != null)
                return false;
        } else if (!appRoot.equals(other.appRoot))
            return false;
        if (bannerContent == null) {
            if (other.bannerContent != null)
                return false;
        } else if (!bannerContent.equals(other.bannerContent))
            return false;
        if (bannerEnabled != other.bannerEnabled)
            return false;
        if (passwordStrengthRequirements == null) {
            if (other.passwordStrengthRequirements != null)
                return false;
        } else if (!passwordStrengthRequirements.equals(other.passwordStrengthRequirements))
            return false;
        if (sessionMaxInactivityInterval != other.sessionMaxInactivityInterval)
            return false;
        if (userConfiguration == null) {
            if (other.userConfiguration != null)
                return false;
        } else if (!userConfiguration.equals(other.userConfiguration))
            return false;
        return true;
    }

}
