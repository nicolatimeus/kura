package org.eclipse.kura.web;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.shared.model.GwtUserData;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

public class UserConfiguration {

    private final String adminUsername;
    private final String adminPassword;

    private final GwtUserData adminUserData;
    private final CryptoService cryptoService;

    private final Map<String, GwtUserData> userData;
    private final Map<String, String> userPasswords;

    public UserConfiguration(final CryptoService cryptoService, final String adminUserName,
            final String adminPassword) {
        this.cryptoService = cryptoService;
        this.adminUsername = adminUserName;
        this.adminPassword = adminPassword;
        this.userData = new HashMap<>();
        this.userPasswords = new HashMap<>();
        this.adminUserData = new GwtUserData(adminUsername, Collections.emptySet(), true);
    }

    public UserConfiguration(final CryptoService cryptoService, final String adminUserName, final String adminPassword,
            final String serializedConfig) {
        final JsonObject object = Json.parse(serializedConfig).asObject();

        this.cryptoService = cryptoService;
        this.adminUsername = adminUserName;
        this.adminPassword = adminPassword;
        this.userData = extractUserData(object.get("permissions").asObject());
        this.userPasswords = extractPasswords(object.get("passwords").asObject());
        this.adminUserData = new GwtUserData(adminUsername, Collections.emptySet(), true);
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

    public void setPassword(final String userName, final String password) {
        requireNonNull(password, "Password cannot be null");
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (adminUsername.equals(userName)) {
            throw new IllegalArgumentException("Admin password must be changed with corresponding property");
        }
        try {
            final String hashedPassword = this.cryptoService.sha1Hash(password);
            this.userPasswords.put(userName, hashedPassword);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to compute password hash", e);
        }
    }

    public void removePassword(final String userName) {
        if (adminUsername.equals(userName)) {
            throw new IllegalArgumentException("Admin password cannot be removed");
        }
        this.userPasswords.remove(userName);
    }

    public void setUserData(final Map<String, GwtUserData> usersData) {
        requireNonNull(userData, "User data cannot be null");
        if (usersData.containsKey(adminUsername)) {
            throw new IllegalArgumentException("Admin user data cannot be changed");
        }
        this.userData.putAll(usersData);
    }

    public void createUser(final String userName) {
        if (userData.containsKey(userName) || adminUsername.contentEquals(userName)) {
            throw new IllegalArgumentException("User already exists");
        }
        this.userData.put(userName, new GwtUserData(userName, Collections.emptySet(), false));
    }

    public void deleteUser(final String userName) {
        if (userName.equals(adminUsername)) {
            throw new IllegalArgumentException("Admin user cannot be deleted");
        }
        this.userPasswords.remove(userName);
        this.userData.remove(userName);
    }

    public Map<String, GwtUserData> getUserData() {
        return Collections.unmodifiableMap(userData);
    }

    public JsonObject toJson() {
        final JsonObject result = new JsonObject();

        final JsonObject passwords = new JsonObject();

        for (final Entry<String, String> e : this.userPasswords.entrySet()) {
            passwords.set(e.getKey(), e.getValue());
        }

        final JsonObject permissions = new JsonObject();

        for (final Entry<String, GwtUserData> data : this.userData.entrySet()) {
            final JsonArray value = new JsonArray();

            for (final String permission : data.getValue().getPermissions()) {
                value.add(permission);
            }

            permissions.add(data.getKey(), value);
        }

        result.add("permissions", permissions);
        result.add("passwords", passwords);

        return result;
    }

    public void store(final ConfigurationService configurationService) throws KuraException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(ConsoleOptions.PROP_USER_CONFIGURATION, toJson().toString());
        configurationService.updateConfiguration("org.eclipse.kura.web.Console", properties);
    }

    private static Map<String, GwtUserData> extractUserData(final JsonObject object) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((adminPassword == null) ? 0 : adminPassword.hashCode());
        result = prime * result + ((adminUsername == null) ? 0 : adminUsername.hashCode());
        result = prime * result + ((userData == null) ? 0 : userData.hashCode());
        result = prime * result + ((userPasswords == null) ? 0 : userPasswords.hashCode());
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
        UserConfiguration other = (UserConfiguration) obj;
        if (adminPassword == null) {
            if (other.adminPassword != null)
                return false;
        } else if (!adminPassword.equals(other.adminPassword))
            return false;
        if (adminUsername == null) {
            if (other.adminUsername != null)
                return false;
        } else if (!adminUsername.equals(other.adminUsername))
            return false;
        if (userData == null) {
            if (other.userData != null)
                return false;
        } else if (!userData.equals(other.userData))
            return false;
        if (userPasswords == null) {
            if (other.userPasswords != null)
                return false;
        } else if (!userPasswords.equals(other.userPasswords))
            return false;
        return true;
    }

}
