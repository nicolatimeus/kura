package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.web.Console;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslAuthenticationServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SslAuthenticationServlet.class);

    /**
     * 
     */
    private static final long serialVersionUID = -2371828320004624864L;

    private final String redirectPath;

    public SslAuthenticationServlet(final String redirectPath) {
        this.redirectPath = redirectPath;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final Console console = Console.instance();
        final HttpSession session = console.createSession(req);

        if (console.getUsername(session).isPresent()) {
            sendRedirect(resp, redirectPath);
            return;
        }

        try {

            @SuppressWarnings("unchecked")
            final Set<TrustAnchor> trustAnchors = (Set<TrustAnchor>) req
                    .getAttribute("org.eclipse.kura.web.trust.anchors");

            final X509Certificate[] clientCertificates = (X509Certificate[]) req
                    .getAttribute("javax.servlet.request.X509Certificate");

            final X509Certificate trustAnchor = getTrustAnchor(trustAnchors, Arrays.asList(clientCertificates))
                    .getTrustedCert();

            final LdapName ldapName = new LdapName(trustAnchor.getSubjectX500Principal().getName());

            final Optional<Rdn> commonNameRdn = ldapName.getRdns().stream()
                    .filter(r -> "cn".equalsIgnoreCase(r.getType())).findAny();

            if (!commonNameRdn.isPresent()) {
                throw new IllegalArgumentException("Root certificate common name is not present");
            }

            final String commonName = (String) commonNameRdn.get().getValue();

            if (Console.getConsoleOptions().getUserData(commonName).isPresent()) {
                console.setAuthenticated(session, commonName);
                sendRedirect(resp, redirectPath);
            } else {
                throw new IllegalArgumentException("Certificate is not associated with an user");
            }

        } catch (final Exception e) {
            logger.warn("certificate authentication failed", e);
            sendUnauthorized(resp);
        }

    }

    private TrustAnchor getTrustAnchor(final Set<TrustAnchor> trustAnchors,
            final List<X509Certificate> clientCertificates) throws CertificateException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, CertPathValidatorException {

        final CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(clientCertificates);
        final CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        final PKIXParameters params = new PKIXParameters(trustAnchors);
        params.setRevocationEnabled(false);
        final PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(path, params);
        return result.getTrustAnchor();
    }

    private void sendUnauthorized(final HttpServletResponse resp) {
        try {
            resp.sendError(401);
        } catch (IOException e) {
            logger.warn("failed to send error", e);
        }
    }

    private void sendRedirect(final HttpServletResponse resp, final String path) {
        try {
            resp.sendRedirect(path);
        } catch (final IOException e) {
            logger.warn("failed to send redirect", e);
        }
    }
}
