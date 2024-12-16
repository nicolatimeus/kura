package org.eclipse.kura.core.configuration.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.core.configuration.ConfigurationSnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropinFolderWatcher implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DropinFolderWatcher.class);

    private static final long UPDATE_DELAY_NANOS = TimeUnit.SECONDS.toNanos(10);

    private final Thread executorThread = new Thread(this::poll);
    private final WatchService watchService;
    private final ConfigurationSnapshotStore.Listener listener;

    public DropinFolderWatcher(final ConfigurationSnapshotStore.Listener listener, final File dropinFolder)
            throws IOException {
        this.listener = listener;
        final Path asPath = dropinFolder.toPath();

        this.watchService = asPath.getFileSystem().newWatchService();

        asPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        this.executorThread.start();
    }

    public void poll() {

        OptionalLong lastEvent = OptionalLong.empty();

        while (!Thread.currentThread().isInterrupted()) {

            if (lastEvent.isPresent() && System.nanoTime() - lastEvent.getAsLong() > UPDATE_DELAY_NANOS) {
                this.listener.onSnapshotsChanged();
                lastEvent = OptionalLong.empty();
            }

            final WatchKey key;

            try {
                if (lastEvent.isPresent()) {
                    final long remainingNanos = lastEvent.getAsLong() + UPDATE_DELAY_NANOS - System.nanoTime();
                    key = this.watchService.poll(Math.max(1, remainingNanos), TimeUnit.NANOSECONDS);
                } else {
                    key = this.watchService.take();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (key != null) {
                key.pollEvents();
                key.reset();
                lastEvent = OptionalLong.of(System.nanoTime());
                logger.info("Configuration snapshot dropins modified, configuration reload scheduled..");
            }
        }

    }

    @Override
    public void close() throws IOException {
        executorThread.interrupt();

        try {
            executorThread.join(TimeUnit.SECONDS.toMillis(30));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        watchService.close();
    }

}
