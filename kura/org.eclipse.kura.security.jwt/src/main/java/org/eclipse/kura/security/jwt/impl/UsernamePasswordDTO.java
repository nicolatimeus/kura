package org.eclipse.kura.security.jwt.impl;

import java.util.Optional;

public class UsernamePasswordDTO {

    private String username;
    private String password;

    public UsernamePasswordDTO(String name, String password) {
        this.username = name;
        this.password = password;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }
}
