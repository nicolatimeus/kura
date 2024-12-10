package org.eclipse.kura.core.configuration;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;

public interface ConfigurationSnapshotStore {

    public SortedSet<Long> getSnapshots() throws KuraException;

    public List<ComponentConfiguration> loadSnapshot(final long id) throws KuraException;

    public long saveSnapshot(final Collection<? extends ComponentConfiguration> snapshotContent) throws KuraException;

    public long saveSnapshot(final Map<String, ComponentConfiguration> modifiedConfigurations,
            final Set<String> deletedPids) throws KuraException;

    public void registerListener(Listener listener);

    public void unregisterListener(Listener listener);

    public interface Listener {

        public void onSnapshotsChanged();
    }
}
