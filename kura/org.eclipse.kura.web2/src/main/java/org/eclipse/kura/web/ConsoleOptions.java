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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.configuration.Property;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.eclipse.kura.web.shared.model.GwtUserInfo;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

public class ConsoleOptions {

    static final String PROP_CONSOLE_USERNAME = "console.username.value";
    static final String PROP_CONSOLE_PASSWORD = "console.password.value";
    static final String PROP_APP_ROOT = "app.root";
    static final String PROP_SESSION_MAX_INACTIVITY_INTERVAL = "session.max.inactivity.interval";
    static final String PROP_ACCESS_BANNER_ENABLED = "access.banner.enabled";
    static final String PROP_ACCESS_BANNER_CONTENT = "access.banner.content";

    private static final String PROP_PW_MIN_LENGTH = "new.password.min.length";
    private static final String PROP_PW_REQUIRE_DIGITS = "new.password.require.digits";
    private static final String PROP_PW_REQUIRE_SPECIAL_CHARS = "new.password.require.special.characters";
    private static final String PROP_PW_REQUIRE_BOTH_CASES = "new.password.require.both.cases";

    private static final String PROP_USER_DATA = "user.data";

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

    private static final Property<String> USER_DATA = new Property<>(PROP_USER_DATA,
            "{\"permissions\": {}, \"passwords\": {}}");

    private final String adminUsername;
    private final String adminPassword;
    private final GwtUserData adminUserData;
    private final String appRoot;
    private final int sessionMaxInactivityInterval;
    private final boolean bannerEnabled;
    private final String bannerContent;
    private final GwtUserInfo userOptions;
    private final Map<String, GwtUserData> userData;
    private final Map<String, String> userPasswords;

    public ConsoleOptions(Map<String, Object> properties, final CryptoService cryptoService)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, KuraException {
        this.adminUsername = CONSOLE_USERNAME.get(properties);
        this.adminPassword = loadAdminPassword(cryptoService, (String) CONSOLE_PASSWORD.get(properties)); // TODO:
                                                                                                          // to
                                                                                                          // Password
                                                                                                          // object?
        this.adminUserData = new GwtUserData(adminUsername, Collections.emptySet(), true);
        this.appRoot = CONSOLE_APP_ROOT.get(properties);
        this.sessionMaxInactivityInterval = SESSION_MAX_INACTIVITY_INTERVAL.get(properties);
        this.bannerEnabled = ACCESS_BANNER_ENABLED.get(properties);
        this.bannerContent = ACCESS_BANNER_CONTENT.get(properties);
        this.userOptions = extractUserOptions(properties);

        Map<String, GwtUserData> permissions;
        Map<String, String> passwords;

        try {
            final JsonObject root = Json.parse(USER_DATA.get(properties)).asObject();
            permissions = extractUserData(root.get("permissions").asObject(), this.adminUsername);
            passwords = extractPasswords(root.get("passwords").asObject());
        } catch (final Exception e) {
            permissions = Collections.emptyMap();
            passwords = Collections.emptyMap();
        }

        this.userData = permissions;
        this.userPasswords = passwords;
    }

    public String getAdminUsername() {
        return this.adminUsername;
    }

    public String getAdminUserPassword() {
        return this.adminPassword;
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

    public GwtUserInfo getUserOptions() {
        return new GwtUserInfo(this.userOptions);
    }

    public Optional<String> getUserPassword(final String userName) {
        if (this.adminUsername.equals(userName)) {
            return Optional.of(this.adminPassword);
        } else {
            return Optional.ofNullable(this.userPasswords.get(userName));
        }
    }

    public GwtUserData getUserDataOrDefault(final String userName) {
        return getUserData(userName).orElse(new GwtUserData(userName, Collections.emptySet(), false));
    }

    public Optional<GwtUserData> getUserData(final String userName) {
        if (this.adminUsername.equals(userName)) {
            return Optional.of(adminUserData);
        } else {
            return Optional.ofNullable(this.userData.get(userName));
        }
    }

    private static String loadAdminPassword(final CryptoService cryptoService, final String password)
            throws KuraException, NoSuchAlgorithmException, UnsupportedEncodingException {
        final char[] decrypted = cryptoService.decryptAes(password.toCharArray());
        return cryptoService.sha1Hash(new String(decrypted));
    }

    private static Map<String, GwtUserData> extractUserData(final JsonObject object, final String adminUsername) {
        final Map<String, GwtUserData> result = new HashMap<>();

        for (final Member member : object) {
            final String userName = member.getName();

            final Set<String> permissions = new HashSet<>();

            for (final JsonValue value : member.getValue().asArray()) {
                permissions.add(value.asString());
            }

            result.put(userName, new GwtUserData(userName, permissions, false));
        }

        return result;
    }

    private static Map<String, String> extractPasswords(final JsonObject object) {
        final Map<String, String> result = new HashMap<>();

        for (final Member member : object) {
            result.put(member.getName(), member.getValue().asString());
        }

        return result;
    }

    private static GwtUserInfo extractUserOptions(final Map<String, Object> properties) {
        final GwtUserInfo result = new GwtUserInfo();

        result.setPasswordMinimumLength(PW_MIN_LENGTH.get(properties));
        result.setPasswordRequireDigits(PW_REQUIRE_DIGITS.get(properties));
        result.setPasswordRequireSpecialChars(PW_REQUIRE_SPECIAL_CHARS.get(properties));
        result.setPasswordRequireBothCases(PW_REQUIRE_BOTH_CASES.get(properties));

        return result;
    }

}
