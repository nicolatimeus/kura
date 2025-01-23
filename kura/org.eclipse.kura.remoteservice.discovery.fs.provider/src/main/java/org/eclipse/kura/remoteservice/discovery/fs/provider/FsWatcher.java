/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.remoteservice.discovery.fs.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FsWatcher implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(FsWatcher.class);

    private final Thread executorThread = new Thread(this::poll);
    private final WatchService watchService;
    private final Listener listener;

    public FsWatcher(final Listener listener, final File root) throws IOException {
        this.listener = listener;
        final Path asPath = root.toPath();

        this.watchService = asPath.getFileSystem().newWatchService();

        asPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        this.executorThread.start();
    }

    public void poll() {

        while (!Thread.currentThread().isInterrupted()) {

            final WatchKey key;

            try {
                key = this.watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            final List<WatchEvent<?>> events = key.pollEvents();

            for (final WatchEvent<?> event : events) {
                final Object context = event.context();

                logger.debug("event: {}, event.context {}", event, event.context());

                if (!(context instanceof Path)) {
                    continue;
                }

                final File file = ((Path) context).toFile();

                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                        || event == StandardWatchEventKinds.ENTRY_MODIFY) {
                    listener.onFileChanged(file);
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    listener.onFileRemoved(file);
                }

            }

            key.reset();
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

    public interface Listener {

        public void onFileChanged(final File path);

        public void onFileRemoved(final File path);

    }

}