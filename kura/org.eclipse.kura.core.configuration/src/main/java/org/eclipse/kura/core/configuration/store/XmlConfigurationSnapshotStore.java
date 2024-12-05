package org.eclipse.kura.core.configuration.store;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.ConfigurationSnapshotStore;
import org.eclipse.kura.core.configuration.XmlComponentConfigurations;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlConfigurationSnapshotStore implements ConfigurationSnapshotStore {

    private static final Logger logger = LoggerFactory.getLogger(XmlConfigurationSnapshotStore.class);
    private static final Pattern SNAPSHOT_FILENAME_PATTERN = Pattern.compile("snapshot_([0-9]+)\\.xml");

    private Marshaller xmlMarshaller;
    private Unmarshaller xmlUnmarshaller;
    private SystemService systemService;
    private CryptoService cryptoService;

    public void setXmlMarshaller(final Marshaller marshaller) {
        this.xmlMarshaller = marshaller;
    }

    public void setXmlUnmarshaller(final Unmarshaller unmarshaller) {
        this.xmlUnmarshaller = unmarshaller;
    }

    public void setSystemService(final SystemService systemService) {
        this.systemService = systemService;
    }

    public void setCryptoService(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate() {

        if (allSnapshotsUnencrypted()) {
            try {
                encryptPlainSnapshots();
            } catch (Exception e) {
                logger.warn("failed to encrypt snapshots", e);
            }
        }

    }

    @Override
    public Set<Long> getSnapshots() {
        // keeps the list of snapshots ordered
        TreeSet<Long> ids = new TreeSet<>();
        String configDir = this.systemService.getKuraSnapshotsDirectory();
        if (configDir != null) {
            File fConfigDir = new File(configDir);
            File[] files = fConfigDir.listFiles();
            if (files != null) {

                for (File file : files) {
                    Matcher m = SNAPSHOT_FILENAME_PATTERN.matcher(file.getName());
                    if (m.matches()) {
                        ids.add(Long.parseLong(m.group(1)));
                    }
                }
            }
        }
        return ids;
    }

    @Override
    public List<ComponentConfiguration> loadSnapshot(final long id) throws KuraException {

        List<ComponentConfiguration> configs = null;

        XmlComponentConfigurations xmlConfigs = loadEncryptedSnapshotFileContent(id);
        if (xmlConfigs != null) {
            configs = xmlConfigs.getConfigurations();
        }

        return configs;
    }

    @Override
    public long saveSnapshot(Collection<? extends ComponentConfiguration> configs) throws KuraException {
        List<ComponentConfiguration> configsToSave = configs.stream()
                .map(cc -> new ComponentConfigurationImpl(cc.getPid(), null, cc.getConfigurationProperties()))
                .collect(Collectors.toList());

        // Build the XML structure
        XmlComponentConfigurations conf = new XmlComponentConfigurations();
        conf.setConfigurations(configsToSave);

        // Write it to disk: marshall
        long sid = new Date().getTime();

        // Do not save the snapshot in the past
        Set<Long> snapshotIDs = getSnapshots();
        if (snapshotIDs != null && !snapshotIDs.isEmpty()) {
            Long[] snapshots = snapshotIDs.toArray(new Long[] {});
            Long lastestID = snapshots[snapshotIDs.size() - 1];

            if (lastestID != null && sid <= lastestID) {
                logger.warn("Snapshot ID: {} is in the past. Adjusting ID to: {} + 1", sid, lastestID);
                sid = lastestID + 1;
            }
        }

        // Write snapshot
        writeSnapshot(sid, conf);

        // Garbage Collector for number of Snapshots Saved
        garbageCollectionOldSnapshots();
        return sid;
    }

    protected <T> T unmarshal(final InputStream in, final Class<T> clazz) throws KuraException {
        try {
            return requireNonNull(this.xmlUnmarshaller.unmarshal(in, clazz));
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, "configuration", e);
        }
    }

    protected void marshal(final OutputStream out, final Object object) throws KuraException {
        try {
            this.xmlMarshaller.marshal(out, object);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR, "configuration", e);
        }
    }

    private File getSnapshotFile(long id) {
        String configDir = this.systemService.getKuraSnapshotsDirectory();

        if (configDir == null) {
            return null;
        }

        StringBuilder sbSnapshot = new StringBuilder(configDir);
        sbSnapshot.append(File.separator).append("snapshot_").append(id).append(".xml");

        String snapshot = sbSnapshot.toString();
        return new File(snapshot);
    }

    private boolean allSnapshotsUnencrypted() {
        Set<Long> snapshotIDs = getSnapshots();
        if (snapshotIDs == null || snapshotIDs.isEmpty()) {
            return false;
        }
        Long[] snapshots = snapshotIDs.toArray(new Long[] {});

        for (Long snapshot : snapshots) {
            try (final FileInputStream in = new FileInputStream(getSnapshotFile(snapshot))) {
                unmarshal(in, XmlComponentConfigurations.class);
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    private XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
        File fSnapshot = getSnapshotFile(snapshotID);
        if (fSnapshot == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, "null");
        }

        InputStream decryptedStream;
        try {
            decryptedStream = this.cryptoService.decryptAes(new FileInputStream(fSnapshot));
        } catch (FileNotFoundException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, fSnapshot.getAbsolutePath());
        }

        XmlComponentConfigurations xmlConfigs = null;

        try (final InputStream in = decryptedStream) {
            xmlConfigs = unmarshal(in, XmlComponentConfigurations.class);
        } catch (Exception e) {
            logger.warn("Error parsing xml", e);
        }

        return xmlConfigs;
    }

    private void encryptPlainSnapshots() throws KuraException, IOException {
        Set<Long> snapshotIDs = getSnapshots();
        if (snapshotIDs == null || snapshotIDs.isEmpty()) {
            return;
        }
        Long[] snapshots = snapshotIDs.toArray(new Long[] {});

        for (Long snapshot : snapshots) {
            File fSnapshot = getSnapshotFile(snapshot);
            if (fSnapshot == null) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, snapshot);
            }

            final XmlComponentConfigurations xmlConfigs;
            try (final InputStream in = new FileInputStream(fSnapshot)) {
                xmlConfigs = unmarshal(in, XmlComponentConfigurations.class);
            } catch (final FileNotFoundException e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, snapshot);
            }

            ComponentUtil.encryptConfigs(xmlConfigs.getConfigurations(), this.cryptoService);

            // Writes an encrypted snapshot with encrypted passwords.
            writeSnapshot(snapshot, xmlConfigs);
        }
    }

    private void writeSnapshot(long sid, XmlComponentConfigurations conf) throws KuraException {
        File fSnapshot = getSnapshotFile(sid);
        if (fSnapshot == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND);
        }

        try (final FileOutputStream fos = new FileOutputStream(fSnapshot);
                final OutputStream encrypted = this.cryptoService.encryptAes(fos)) {
            logger.info("Writing snapshot - Saving {}...", fSnapshot.getAbsolutePath());
            marshal(encrypted, conf);
            encrypted.flush();
            fos.flush();
            fos.getFD().sync();
            logger.info("Writing snapshot - Saving {}... Done.", fSnapshot.getAbsolutePath());
        } catch (KuraException e) {
            throw e;
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private void garbageCollectionOldSnapshots() {
        // get the current snapshots and compared with the maximum number we
        // need to keep
        TreeSet<Long> sids = (TreeSet<Long>) getSnapshots();

        int currCount = sids.size();
        int maxCount = this.systemService.getKuraSnapshotsCount();
        while (currCount > maxCount && !sids.isEmpty()) { // stop if count reached or no more snapshots remain

            // preserve snapshot ID 0 as this will be considered the seeding
            // one.
            long sid = sids.pollFirst();
            File fSnapshot = getSnapshotFile(sid);
            if (sid == 0 || fSnapshot == null) {
                continue;
            }

            Path fSnapshotPath = fSnapshot.toPath();
            try {
                if (Files.deleteIfExists(fSnapshotPath)) {
                    logger.info("Snapshots Garbage Collector. Deleted {}", fSnapshotPath);
                    currCount--;
                }
            } catch (IOException e) {
                logger.warn("Snapshots Garbage Collector. Deletion failed for {}", fSnapshotPath, e);
            }
        }
    }

}
