package org.eclipse.kura.security.jwt.impl;

import java.security.Principal;
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.eclipse.kura.security.jwt.JwtService;

import com.auth0.jwt.interfaces.DecodedJWT;

@Priority(50)
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private static final Pattern SPACE = Pattern.compile(" ");

    private JwtService jwtService;

    public void setJwtService(final JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onEnabled() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDisabled() {
        // TODO Auto-generated method stub
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {

        try {

            final String header = request.getHeader("Authorization");

            final Iterator<String> parts = SPACE.splitAsStream(header).filter(s -> !s.isEmpty()).iterator();

            parts.next();
            final String token = parts.next();

            final DecodedJWT decodedToken = jwtService.decodeAndVerifyToken(token);

            return Optional.ofNullable(decodedToken.getSubject()).map(t -> () -> t);

        } catch (final Exception e) {
            return Optional.empty();
        }

    }

}
