package org.eclipse.kura.core.configuration.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableFile;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedXmlConfigurationSnapshotStore extends XmlConfigurationSnapshotStore {

    private static final Logger logger = LoggerFactory.getLogger(SignedXmlConfigurationSnapshotStore.class);

    private KeystoreService keystoreService;

    public void setKeystoreService(final KeystoreService keystoreService) {
        this.keystoreService = keystoreService;
    }

    @Override
    protected List<ComponentConfiguration> loadConfigurationDropin(final File dropinFile) throws KuraException {
        try {
            checkSignature(dropinFile);
            logger.warn("signature verification for {} succeded", dropinFile);
        } catch (final Exception e) {
            logger.warn("signature verification for {} falied", dropinFile, e);
        }

        return super.loadConfigurationDropin(dropinFile);
    }

    private void checkSignature(final File dropinFile) throws KuraException {
        final File signature = new File(dropinFile.getParent(), dropinFile.getName() + ".p7b");

        if (!signature.exists()) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, "Signature file not found");
        }

        final CMSSignedData signedData;

        try (final FileInputStream in = new FileInputStream(signature)) {
            signedData = new CMSSignedData(new CMSProcessableFile(dropinFile), in);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e, "IO error loading signature file");
        } catch (CMSException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e, "Failed to decode signature file");
        }

        final SignerInformationStore signerInfoStore = signedData.getSignerInfos();

        boolean verified = false;

        final List<X509Certificate> trustedCertificates = getTrustedCertificates();
        final List<X509Certificate> signedDataCertificates = toX509CertificateList(signedData.getCertificates());

        for (final SignerInformation signerInfo : signerInfoStore.getSigners()) {

            Optional<X509Certificate> signerCertificate = trustedCertificates.stream()
                    .filter(c -> matches(c, signerInfo.getSID())).findAny();

            if (!signerCertificate.isPresent()) {

                signerCertificate = findTrustedSignerCertificate(signerInfo.getSID(), trustedCertificates,
                        signedDataCertificates);

            }

            if (signerCertificate.isPresent() && verifySignature(signerInfo, signerCertificate.get())) {
                verified = true;
                break;
            }

        }

        if (!verified) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, "cannot verify signature for " + dropinFile);
        }
    }

    private boolean verifySignature(final SignerInformation signerInfo, final X509Certificate signerCertificate) {
        final SignerInformationVerifier verifier;

        try {
            verifier = new JcaSimpleSignerInfoVerifierBuilder().build(signerCertificate);
        } catch (OperatorCreationException e) {
            logger.warn("failed to create verifier", e);
            return false;
        }

        try {
            return signerInfo.verify(verifier);
        } catch (CMSException e) {
            logger.warn("unexpected exception verifying signature", e);
            return false;
        }

    }

    private Optional<X509Certificate> findTrustedSignerCertificate(final SignerId id,
            final Collection<X509Certificate> trustedCertificates,
            final Collection<X509Certificate> signedDataCertificates) {
        final Optional<X509Certificate> candidate = signedDataCertificates.stream().filter(c -> matches(c, id))
                .findAny();

        if (candidate.isPresent()) {
            try {
                buildCertificateChain(candidate.get(), trustedCertificates, signedDataCertificates);
            } catch (final Exception e) {
                return Optional.empty();
            }
        }

        return candidate;
    }

    private void buildCertificateChain(final X509Certificate target,
            final Collection<X509Certificate> trustedCertificates,
            final Collection<X509Certificate> additionalCertificates) throws KuraException {
        try {
            final CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");

            final X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(target);

            final PKIXBuilderParameters params = new PKIXBuilderParameters(
                    trustedCertificates.stream().map(c -> new TrustAnchor(c, null)).collect(Collectors.toSet()),
                    selector);

            final CertStore certStore = CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters(additionalCertificates));

            params.addCertStore(certStore);
            params.setRevocationEnabled(false);

            builder.build(params);

        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, e);
        }

    }

    private List<X509Certificate> getTrustedCertificates() throws KuraException {
        return this.keystoreService.getEntries().values().stream()
                .filter(e -> e instanceof TrustedCertificateEntry
                        && ((TrustedCertificateEntry) e).getTrustedCertificate() instanceof X509Certificate)
                .map(e -> (X509Certificate) ((TrustedCertificateEntry) e).getTrustedCertificate())
                .collect(Collectors.toList());
    }

    private List<X509Certificate> toX509CertificateList(final Store<X509CertificateHolder> store) {
        final List<X509Certificate> result = new ArrayList<>();

        final Collection<X509CertificateHolder> certs = store.getMatches(new AnyCertificateSelector());

        for (final X509CertificateHolder c : certs) {
            try {
                result.add(new JcaX509CertificateConverter().getCertificate(c));
            } catch (CertificateException e) {
                logger.warn("Unsupported certificate in signature");
            }
        }

        return result;
    }

    private final boolean matches(final X509Certificate cert, final SignerId id) {

        if (id.getSerialNumber() == null && id.getIssuer() == null && id.getSubjectKeyIdentifier() == null) {
            return false;
        }

        final X509CertSelector selector = new X509CertSelector();

        selector.setSerialNumber(id.getSerialNumber());
        if (id.getIssuer() != null) {
            try {
                selector.setIssuer(new X500Principal(id.getIssuer().getEncoded()));
            } catch (IOException e) {
                logger.warn("failed to construct issuer principal", e);
                return false;
            }
        }
        selector.setSubjectKeyIdentifier(id.getSubjectKeyIdentifier());

        return selector.match(cert);
    }

    private static class AnyCertificateSelector implements Selector<X509CertificateHolder> {

        @Override
        public Object clone() {
            return new AnyCertificateSelector();
        }

        @Override
        public boolean match(X509CertificateHolder arg0) {
            return true;
        }

    }

}
