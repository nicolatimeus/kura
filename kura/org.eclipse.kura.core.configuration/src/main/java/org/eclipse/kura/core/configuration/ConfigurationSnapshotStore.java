package org.eclipse.kura.core.configuration;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;

public interface ConfigurationSnapshotStore {

    public Set<Long> getSnapshots() throws KuraException;

    public List<ComponentConfiguration> loadSnapshot(final long id) throws KuraException;

    public long saveSnapshot(final Collection<? extends ComponentConfiguration> configs) throws KuraException;
}
