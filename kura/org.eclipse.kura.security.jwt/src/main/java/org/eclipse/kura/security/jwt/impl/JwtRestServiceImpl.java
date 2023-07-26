package org.eclipse.kura.security.jwt.impl;

import static java.util.Objects.isNull;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.security.jwt.JwtService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;

@Path("jwt/v1")
public class JwtRestServiceImpl {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String TOKEN_ID_CLAIM = "tokenId";
    private static final String PASSWORD_AUTH_FAILED_MSG = "{} Rest - Failure - Authentication failed as username or password not matching";
    private static final String KURA_USER_PREFIX = "kura.user.";
    private static final String KURA_NEED_PASSWORD_CHANGE = "kura.need.password.change";
    private static final String KURA_PASSWORD_CREDENTIAL = "kura.password";

    private static final Logger logger = LoggerFactory.getLogger(JwtRestServiceImpl.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private UserAdmin userAdmin;
    private CryptoService cryptoService;
    private JwtService jwtService;

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    public void setCryptoService(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void setJwtService(final JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Context
    private HttpServletRequest req;

    @Context
    private HttpServletResponse resp;

    @POST
    @Path("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String authenticateWithUsernameAndPassword(final UsernamePasswordDTO credentials)
            throws WebApplicationException {

        final AuditContext auditContext = AuditContext.currentOrInternal();

        final String username = credentials.getUsername().orElseThrow(() -> new WebApplicationException(400));
        final String password = credentials.getPassword().orElseThrow(() -> new WebApplicationException(400));

        auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), username);

        final Role userRole = userAdmin.getRole(KURA_USER_PREFIX + username);

        if (!(userRole instanceof User)) {
            auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
            throw new WebApplicationException(401);
        }

        final User user = (User) userRole;

        if ("true".equals(user.getProperties().get(KURA_NEED_PASSWORD_CHANGE))) {
            throw new WebApplicationException(401);
        }

        final String storedPasswordHash = (String) user.getCredentials().get(KURA_PASSWORD_CREDENTIAL);

        if (isNull(storedPasswordHash)) {
            auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
            throw new WebApplicationException(401);
        }

        try {
            if (cryptoService.sha256Hash(password).equals(storedPasswordHash)) {
                auditLogger.info("{} Rest - Success - Authentication succeeded via password provider", auditContext);
                return buildJwtToken(username);
            } else {
                auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
                throw new WebApplicationException(401);
            }
        } catch (final WebApplicationException e) {
            throw e;
        } catch (final Exception e) {
            auditLogger.warn(PASSWORD_AUTH_FAILED_MSG, auditContext);
            logger.warn("Failed to compute password hash", e);
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("certificate")
    @Produces(MediaType.APPLICATION_JSON)
    public String authenticateWithCertificacte() {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        try {

            final Object clientCertificatesRaw = req.getAttribute("javax.servlet.request.X509Certificate");

            if (!(clientCertificatesRaw instanceof X509Certificate[])) {
                throw new WebApplicationException(401);
            }

            final X509Certificate[] clientCertificates = (X509Certificate[]) clientCertificatesRaw;

            if (clientCertificates.length == 0) {
                throw new IllegalArgumentException("Certificate chain is empty");
            }

            final LdapName ldapName = new LdapName(clientCertificates[0].getSubjectX500Principal().getName());

            final Optional<Rdn> commonNameRdn = ldapName.getRdns().stream()
                    .filter(r -> "cn".equalsIgnoreCase(r.getType())).findAny();

            if (!commonNameRdn.isPresent()) {
                throw new IllegalArgumentException("Certificate common name is not present");
            }

            final String commonName = (String) commonNameRdn.get().getValue();

            auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), commonName);

            if (this.userAdmin.getRole(KURA_USER_PREFIX + commonName) instanceof User) {
                auditLogger.info("{} Rest - Success - Certificate Authentication succeeded", auditContext);
                return buildJwtToken(commonName);
            }

            auditLogger.warn("{} Rest - Failure - Certificate Authentication failed", auditContext);
            throw new WebApplicationException(401);

        } catch (final WebApplicationException e) {
            throw e;
        } catch (final Exception e) {
            auditLogger.warn("{} Rest - Failure - Certificate Authentication failed", auditContext);
            throw new WebApplicationException(500);
        }
    }

    @POST
    @Path("refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String refresh(final String token) {

        try {

            final Cookie refreshTokenCookie = Arrays.stream(req.getCookies())
                    .filter(c -> REFRESH_TOKEN_COOKIE_NAME.equals(c.getName())).findAny()
                    .orElseThrow(() -> new WebApplicationException(401));

            final DecodedJWT accessToken = jwtService.decodeAndVerifyToken(token);
            final DecodedJWT refreshToken = jwtService.decodeAndVerifyToken(refreshTokenCookie.getValue());

            final String tokenHash = cryptoService.sha256Hash(token);

            if (!tokenHash.equals(refreshToken.getClaim(TOKEN_ID_CLAIM).asString())) {
                throw new WebApplicationException(401);
            }

            return buildJwtToken(accessToken.getSubject());

        } catch (final WebApplicationException e) {
            throw e;
        } catch (final Exception e) {
            throw new WebApplicationException(401);
        }

    }

    private final String buildJwtToken(final String identity)
            throws KuraException, NoSuchAlgorithmException, UnsupportedEncodingException {

        final Instant now = Instant.now();
        final Instant expiration = now.plus(Duration.ofMinutes(1));

        final JWTCreator.Builder builder = JWT.create().withSubject(identity).withExpiresAt(expiration);

        final String accessToken = jwtService.signToken(builder);

        final String tokenHash = cryptoService.sha256Hash(accessToken);

        final JWTCreator.Builder refreshTokenBuilder = JWT.create().withClaim(TOKEN_ID_CLAIM, tokenHash)
                .withExpiresAt(expiration);

        final String refreshToken = jwtService.signToken(refreshTokenBuilder);

        final Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("jwt/v1");
        cookie.setMaxAge((int) Duration.between(now, expiration).getSeconds());

        resp.addCookie(cookie);

        return accessToken;
    }
}
