package org.eclipse.kura.remoteservice.discovery.fs.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@Component(immediate = true)
public class FsDiscoveryProvider implements FsWatcher.Listener {

    private static final Gson GSON = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(FsDiscoveryProvider.class);

    private final EndpointEventListenerDispatcher dispatcher = new EndpointEventListenerDispatcher();
    private final List<EndpointEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ServiceRegistration<EndpointEventListener> localEventListener;
    private final String frameworkUUID;
    private final FsWatcher watcher;
    private final File storageRoot;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void setEndpointEventListener(final EndpointEventListener listener) {
        this.listeners.add(listener);
    }

    public void unsetEndpointEventListener(final EndpointEventListener listener) {
        this.listeners.remove(listener);
    }

    @Activate
    public FsDiscoveryProvider(final @Reference SystemService systemService) {
        final BundleContext context = FrameworkUtil.getBundle(FsDiscoveryProvider.class).getBundleContext();
        this.frameworkUUID = context.getProperty(Constants.FRAMEWORK_UUID);

        this.localEventListener = new LocalEndpointListner().register(context);

        this.storageRoot = new File(systemService.getProperties().getProperty(
                "org.eclipse.kura.remoteservice.discovery.fs.provider.root", "/var/run/kura/remoteservices"));

        if (!this.storageRoot.isDirectory()) {
            try {
                Files.createDirectories(this.storageRoot.toPath());
            } catch (IOException e) {
                throw new ComponentException("failed to create storage root", e);
            }
        }

        try {
            this.watcher = new FsWatcher(this, storageRoot);
        } catch (IOException e) {
            throw new ComponentException("failed to open filesystem watcher", e);
        }

        try (final Stream<Path> stream = Files.list(this.storageRoot.toPath())) {
            stream.map(Path::toFile).filter(f -> f.getName().endsWith("json")).forEach(this::dispatchEndpointChanged);
        } catch (IOException e) {
            logger.warn("failed to list dir", e);
        }

    }

    @Deactivate
    public void deactivate() {
        this.localEventListener.unregister();
        try {
            this.watcher.close();
        } catch (IOException e) {
            logger.warn("failed to close fs watcher", e);
        }
    }

    private static String getDescriptorFileName(final EndpointDescription description) {
        return Utils.getId(description) + ".json";
    }

    private File getDescriptorFilePath(final EndpointDescription description) {
        return new File(this.storageRoot, getDescriptorFileName(description));
    }

    private void updateLocalDescriptor(final EndpointDescription description) {
        final String fileName = getDescriptorFileName(description);

        final File tempFile = new File(this.storageRoot, fileName + "_");
        final File destFile = new File(this.storageRoot, fileName);

        try (final FileOutputStream out = new FileOutputStream(tempFile);
                final Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            GSON.toJson(new EndpointDescriptionDTO(description), writer);
            out.flush();

            Files.move(tempFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (final Exception e) {
            removeFile(tempFile);
            logger.warn("failed to update {}", destFile, e);
        }

    }

    private void removeLocalDescriptor(final EndpointDescription description) {
        removeFile(getDescriptorFilePath(description));
    }

    private void removeFile(final File file) {
        try {
            Files.delete(file.toPath());
        } catch (final IOException e) {
            logger.warn("failed to delete {}", file, e);
        }
    }

    private class LocalEndpointListner implements EndpointEventListener {

        @Override
        public void endpointChanged(final EndpointEvent event, final String filter) {
            switch (event.getType()) {
            case EndpointEvent.ADDED:
            case EndpointEvent.MODIFIED:
                updateLocalDescriptor(event.getEndpoint());
                break;
            case EndpointEvent.REMOVED:
            case EndpointEvent.MODIFIED_ENDMATCH:
                removeLocalDescriptor(event.getEndpoint());
                break;
            default:
                logger.warn("unexpected event type {}", event.getType());
            }
        }

        ServiceRegistration<EndpointEventListener> register(final BundleContext bundleContext) {
            final String filter = "(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=" + frameworkUUID + ")";
            final Map<String, Object> properties = new HashMap<>();
            properties.put(EndpointEventListener.ENDPOINT_LISTENER_SCOPE, new String[] { filter });
            return bundleContext.registerService(EndpointEventListener.class, this,
                    FrameworkUtil.asDictionary(properties));
        }

    }

    @Override
    public void onFileChanged(final File path) {
        if (!path.getName().endsWith("json")) {
            return;
        }

        dispatchEndpointChanged(new File(this.storageRoot, path.getName()));

    }

    private void dispatchEndpointChanged(final File path) {
        try (final FileInputStream in = new FileInputStream(path);
                final Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            final EndpointDescriptionDTO endpointDTO = GSON.fromJson(r, EndpointDescriptionDTO.class);

            final EndpointDescription endpoint = endpointDTO.getEndpointDescription(true);

            if (endpoint.getFrameworkUUID().equals(this.frameworkUUID)) {
                return;
            }

            dispatcher.endpointChanged(endpoint);

        } catch (final Exception e) {
            logger.warn("failed to parse endpoint descriptor", e);
        }
    }

    @Override
    public void onFileRemoved(final File path) {
        final String name = path.getName();

        if (!name.endsWith(".json")) {
            return;
        }

        final String id = name.substring(0, name.length() - 5);

        dispatcher.endpointRemoved(id);
    }

}
