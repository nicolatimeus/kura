package org.eclipse.kura.security.jwt.impl;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.jwt.JwtService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JwtServiceImpl implements JwtService, ConfigurableComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    private Optional<KeystoreService> keystoreService = Optional.empty();
    private Optional<String> keystoreServicPid = Optional.empty();
    private Optional<KeystoreState> state = Optional.empty();

    public void activate() {
        logger.info("JwtServiceImpl activating");
    }

    public void update() {
        logger.info("JwtServiceImpl updating");
    }

    public void setKeystoreService(final KeystoreService keystoreService, final Map<String, Object> properties) {
        this.keystoreService = Optional.of(keystoreService);
        this.keystoreServicPid = Optional.ofNullable(properties.get(ConfigurationService.KURA_SERVICE_PID))
                .filter(s -> s instanceof String).map(String.class::cast);
    }

    public void unsetKeystoreService(final KeystoreService keystoreService) {
        if (Optional.of(keystoreService).equals(this.keystoreService)) {
            this.keystoreService = Optional.empty();
            this.state = Optional.empty();
        }
    }

    @Override
    public String signToken(final JWTCreator.Builder builder) throws KuraException {
        return getState().signToken(builder);
    }

    @Override
    public DecodedJWT decodeAndVerifyToken(final String token) throws KuraException {
        return getState().decodeAndVerifyToken(token);
    }

    private KeystoreService getKeystoreService() throws KuraException {
        return this.keystoreService.orElseThrow(() -> KuraException.internalError("KeystoreService is not bound"));
    }

    private KeystoreState getState() throws KuraException {
        final Optional<KeystoreState> currentState = this.state;

        if (currentState.isPresent()) {
            return currentState.get();
        }

        final KeystoreState newState = new KeystoreState(getKeystoreService());

        this.state = Optional.of(newState);

        return newState;
    }

    @Override
    public void handleEvent(final Event event) {
        if (!(event instanceof KeystoreChangedEvent)) {
            return;
        }

        final KeystoreChangedEvent keystoreChangedEvent = (KeystoreChangedEvent) event;

        final Optional<String> eventPid = Optional.ofNullable(keystoreChangedEvent.getSenderPid());

        if (this.keystoreServicPid.equals(eventPid)) {
            logger.info("Keystore changed, dropping state");
            this.state = Optional.empty();
        }
    }

    private static class KeystoreState {

        private final List<PublicKey> verificationKeys;
        private final Optional<Algorithm> signingAlgorithm;

        KeystoreState(final KeystoreService keystoreService) throws KuraException {

            final List<PublicKey> probedVerificationKeys = new ArrayList<>();
            Optional<Algorithm> probedSigningAlgorithm = Optional.empty();

            for (final KeyStore.Entry e : keystoreService.getEntries().values()) {

                if (e instanceof TrustedCertificateEntry) {

                    probedVerificationKeys.add(((TrustedCertificateEntry) e).getTrustedCertificate().getPublicKey());

                } else if (e instanceof PrivateKeyEntry) {
                    final PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) e;

                    probedVerificationKeys.add(privateKeyEntry.getCertificate().getPublicKey());

                    try {
                        probedSigningAlgorithm = Optional.of(getSigningAlgorithm(privateKeyEntry));
                    } catch (final Exception ex) {
                        logger.warn("failed to construct signing algorithm", ex);
                    }
                }
            }

            this.verificationKeys = probedVerificationKeys;
            this.signingAlgorithm = probedSigningAlgorithm;
        }

        private static final Algorithm getSigningAlgorithm(final PrivateKeyEntry entry) throws KuraException {

            final PrivateKey privateKey = entry.getPrivateKey();
            final PublicKey publicKey = Optional.of(entry.getCertificate()).filter(c -> c instanceof X509Certificate)
                    .map(c -> ((X509Certificate) c).getPublicKey())
                    .orElseThrow(() -> KuraException.internalError("Signing certificate is not an X509Certificate"));

            if (privateKey instanceof RSAPrivateKey && publicKey instanceof RSAPublicKey) {
                return Algorithm.RSA256((RSAPublicKey) publicKey, (RSAPrivateKey) privateKey);
            } else if (privateKey instanceof ECPrivateKey && publicKey instanceof ECPublicKey) {
                return Algorithm.ECDSA256((ECPublicKey) publicKey, (ECPrivateKey) privateKey);
            }

            throw KuraException.internalError("Unsupported key pair");
        }

        private static final Optional<Algorithm> getVerificationAlgorithm(final PublicKey publicKey,
                final String tokenAlgorithm) {

            switch (tokenAlgorithm) {
            case "RS256":
                return asRSAPublicKey(publicKey).map(k -> Algorithm.RSA256(k, null));
            case "RS384":
                return asRSAPublicKey(publicKey).map(k -> Algorithm.RSA384(k, null));
            case "RS512":
                return asRSAPublicKey(publicKey).map(k -> Algorithm.RSA512(k, null));
            case "ES256":
                return asECDSAPublicKey(publicKey).map(k -> Algorithm.ECDSA256(k, null));
            case "ES384":
                return asECDSAPublicKey(publicKey).map(k -> Algorithm.ECDSA384(k, null));
            case "ES512":
                return asECDSAPublicKey(publicKey).map(k -> Algorithm.ECDSA512(k, null));
            default:
                return Optional.empty();
            }
        }

        private static final Optional<RSAPublicKey> asRSAPublicKey(final PublicKey key) {
            if (key instanceof RSAPublicKey) {
                return Optional.of((RSAPublicKey) key);
            } else {
                return Optional.empty();
            }
        }

        private static final Optional<ECPublicKey> asECDSAPublicKey(final PublicKey key) {
            if (key instanceof ECPublicKey) {
                return Optional.of((ECPublicKey) key);
            } else {
                return Optional.empty();
            }
        }

        DecodedJWT decodeAndVerifyToken(final String token) throws KuraException {
            try {
                final DecodedJWT result = JWT.decode(token);

                for (final PublicKey publicKey : verificationKeys) {
                    final String tokenAlgorithm = result.getAlgorithm();

                    final Optional<Algorithm> algorithm = getVerificationAlgorithm(publicKey, tokenAlgorithm);

                    if (!algorithm.isPresent()) {
                        continue;
                    }

                    try {
                        JWT.require(algorithm.get()).build().verify(result);
                        return result;
                    } catch (final Exception e) {
                        continue;
                    }
                }

                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);

            } catch (final JWTDecodeException e) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            } catch (final KuraException e) {
                throw e;
            } catch (final Exception e) {
                throw KuraException.internalError(e);
            }
        }

        String signToken(final JWTCreator.Builder builder) throws KuraException {
            try {

                final Algorithm algorithm = this.signingAlgorithm
                        .orElseThrow(() -> KuraException.internalError("no signing keys available"));

                return builder.sign(algorithm);
            } catch (final KuraException e) {
                throw e;
            } catch (final Exception e) {
                throw KuraException.internalError(e);
            }
        }
    }

}
