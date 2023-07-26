package org.eclipse.kura.security.jwt;

import org.eclipse.kura.KuraException;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;

public interface JwtService {

    public String signToken(final JWTCreator.Builder builder) throws KuraException;

    public DecodedJWT decodeAndVerifyToken(final String token) throws KuraException;
}
