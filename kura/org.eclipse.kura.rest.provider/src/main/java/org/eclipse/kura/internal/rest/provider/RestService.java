package org.eclipse.kura.internal.rest.provider;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.jaxrs.provider.security.AuthenticationHandler;
import com.eclipsesource.jaxrs.provider.security.AuthorizationHandler;

public class RestService implements ConfigurableComponent, AuthenticationHandler, AuthorizationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);
    private static final Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Response UNAUTHORIZED_RESPONSE = Response.status(Response.Status.UNAUTHORIZED)
            .header("WWW-Authenticate", "Basic realm=\"kura-rest-api\"").build();

    private Map<String, User> users;

    private CryptoService cryptoService;

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate(Map<String, Object> properties) {
        logger.info("activating...");
        updated(properties);
        logger.info("activating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");
        logger.info("deactivating...done");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updating...");
        this.users = User.fromOptions(new RestManagerOptions(properties));
        logger.info("updating...done");
    }

    @Override
    public boolean isUserInRole(Principal requestUser, String role) {
        return ((User) requestUser).getRoles().contains(role);
    }

    @Override
    public Principal authenticate(ContainerRequestContext request) {
        String authHeader = request.getHeaderString("Authorization");
        if (authHeader == null) {
            request.abortWith(UNAUTHORIZED_RESPONSE);
            return null;
        }

        StringTokenizer tokens = new StringTokenizer(authHeader);
        String authScheme = tokens.nextToken();
        if (!"Basic".equals(authScheme)) {
            request.abortWith(UNAUTHORIZED_RESPONSE);
            return null;
        }

        final String credentials = new String(BASE64_DECODER.decode(tokens.nextToken()), StandardCharsets.UTF_8);

        int colon = credentials.indexOf(':');
        String userName = credentials.substring(0, colon);
        String requestPassword = credentials.substring(colon + 1);

        final User user = users.get(userName);

        try {
            final char[] userPassword = user.getPassword().getPassword();
            if (userPassword.length == 0 && requestPassword.isEmpty()) {
                return user;
            }
            if (Arrays.equals(userPassword, cryptoService.encryptAes(requestPassword.toCharArray()))) {
                return user;
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }

}
