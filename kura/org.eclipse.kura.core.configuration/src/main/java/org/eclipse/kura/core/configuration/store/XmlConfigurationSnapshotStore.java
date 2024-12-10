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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private Optional<DropinFolderWatcher> watcher = Optional.empty();

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

        final Optional<File> dropinsDir = getDropinDirectory();

        if (dropinsDir.isPresent()) {

            try {
                this.watcher = Optional.of(new DropinFolderWatcher(this::dispatchSnapshotsChanged, dropinsDir.get()));
            } catch (final Exception e) {
                logger.warn("Failed to initialise dropin directory monitoring", e);
            }

        }

    }

    public void deactivate() {

        final Optional<DropinFolderWatcher> currentWatcher = this.watcher;

        if (currentWatcher.isPresent()) {
            try {
                currentWatcher.get().close();
            } catch (Exception e) {
                logger.warn("Failed to shutdown dropin directory monitoring", e);
            }
        }
    }

    @Override
    public SortedSet<Long> getSnapshots() {
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
        ids.add(0L);
        return ids;
    }

    @Override
    public List<ComponentConfiguration> loadSnapshot(final long id) throws KuraException {

        List<ComponentConfiguration> snapshotConfigs;

        try {
            final XmlComponentConfigurations xmlConfigs = loadEncryptedSnapshotFileContent(id);
            snapshotConfigs = xmlConfigs.getConfigurations();
        } catch (final KuraException e) {
            if (e.getCode() == KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND) {
                snapshotConfigs = Collections.emptyList();
            } else {
                throw e;
            }
        }

        final Map<String, ComponentConfiguration> dropinConfigurations = loadDropinConfigurations();

        if (!dropinConfigurations.isEmpty()) {
            ComponentUtil.merge(dropinConfigurations, snapshotConfigs);

            return new ArrayList<>(dropinConfigurations.values());
        } else {
            return snapshotConfigs;
        }
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
        SortedSet<Long> snapshotIDs = getSnapshots();
        if (snapshotIDs != null && !snapshotIDs.isEmpty()) {
            Long lastestID = snapshotIDs.last();

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

    @Override
    public long saveSnapshot(Map<String, ComponentConfiguration> modifiedConfigurations, Set<String> deletedPids)
            throws KuraException {

        final Optional<Long> latestSnapshot = Optional.ofNullable(getSnapshots()).filter(s -> !s.isEmpty())
                .map(SortedSet::last);

        if (latestSnapshot.isPresent() && getSnapshotFile(latestSnapshot.get()).exists()) {
            final Map<String, ComponentConfiguration> latestSnapshotConfigs = ComponentUtil
                    .toMap(loadEncryptedSnapshotFileContent(latestSnapshot.get()).getConfigurations());

            ComponentUtil.merge(latestSnapshotConfigs, modifiedConfigurations.values());
            latestSnapshotConfigs.keySet().removeIf(deletedPids::contains);

            return saveSnapshot(latestSnapshotConfigs.values());
        } else {
            return saveSnapshot(modifiedConfigurations.values());
        }
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
            throw new IllegalStateException("snapshot directory is not set");
        }

        StringBuilder sbSnapshot = new StringBuilder(configDir);
        sbSnapshot.append(File.separator).append("snapshot_").append(id).append(".xml");

        String snapshot = sbSnapshot.toString();
        return new File(snapshot);
    }

    private boolean allSnapshotsUnencrypted() {
        Set<Long> snapshotIDs = getSnapshots();
        if (snapshotIDs.isEmpty()) {
            return false;
        }

        for (Long snapshot : snapshotIDs) {
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

        InputStream decryptedStream;
        try {
            decryptedStream = this.cryptoService.decryptAes(new FileInputStream(fSnapshot));
        } catch (FileNotFoundException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, fSnapshot.getAbsolutePath());
        }

        try (final InputStream in = decryptedStream) {
            return unmarshal(in, XmlComponentConfigurations.class);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e);
        }
    }

    private void encryptPlainSnapshots() throws KuraException, IOException {
        Set<Long> snapshotIDs = getSnapshots();

        for (Long snapshot : snapshotIDs) {
            File fSnapshot = getSnapshotFile(snapshot);
            if (fSnapshot == null) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, snapshot);
            }

            final List<ComponentConfiguration> configs = loadUnencryptedSnapshot(fSnapshot);

            final XmlComponentConfigurations xmlConfigs = new XmlComponentConfigurations();
            xmlConfigs.setConfigurations(configs);

            // Writes an encrypted snapshot with encrypted passwords.
            writeSnapshot(snapshot, xmlConfigs);
        }
    }

    private List<ComponentConfiguration> loadUnencryptedSnapshot(File snapshotFile) throws KuraException, IOException {
        final XmlComponentConfigurations xmlConfigs;
        try (final InputStream in = new FileInputStream(snapshotFile)) {
            xmlConfigs = unmarshal(in, XmlComponentConfigurations.class);
        } catch (final FileNotFoundException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, snapshotFile);
        }

        ComponentUtil.encryptConfigs(xmlConfigs.getConfigurations(), this.cryptoService);

        return xmlConfigs.getConfigurations();
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

    private Optional<File> getDropinDirectory() {
        final String configDir = this.systemService.getKuraSnapshotsDirectory();

        if (configDir == null) {
            return Optional.empty();
        }

        final File result = new File(configDir + ".d");

        if (!result.exists()) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    private final Map<String, ComponentConfiguration> loadDropinConfigurations() throws KuraException {
        final Optional<File> dropinsDir = getDropinDirectory();

        if (!dropinsDir.isPresent()) {
            return Collections.emptyMap();
        }

        final SortedSet<File> files = new TreeSet<>(Comparator.comparing(Function.<File>identity()).reversed());

        try (final Stream<Path> dropinFolderFiles = Files.list(dropinsDir.get().toPath())) {
            dropinFolderFiles.map(Path::toFile).filter(f -> f.getName().endsWith(".xml")).forEach(files::add);
        } catch (final IOException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e);
        }

        final Map<String, ComponentConfiguration> configs = new HashMap<>();

        for (final File f : files) {
            try {
                final List<ComponentConfiguration> snapshot = loadUnencryptedSnapshot(f);

                ComponentUtil.merge(configs, snapshot);
            } catch (final Exception e) {
                logger.warn("failed to load dropin {}", f, e);
            }
        }

        return configs;

    }

    @Override
    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    private void dispatchSnapshotsChanged() {
        for (final Listener listener : this.listeners) {
            try {
                listener.onSnapshotsChanged();
            } catch (final Exception e) {
                logger.warn("unexpected exception to dispatch snapshot changed event", e);
            }
        }
    }

}
