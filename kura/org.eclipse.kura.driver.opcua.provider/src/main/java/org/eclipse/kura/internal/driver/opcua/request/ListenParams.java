package org.eclipse.kura.internal.driver.opcua.request;

import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public abstract class ListenParams extends ReadParams {

    public ListenParams(final Map<String, Object> channelConfig) {
        super(channelConfig);
    }

    public ListenParams(final ReadValueId readValueId) {
        super(readValueId);
    }

    public abstract double getSamplingInterval();

    public abstract long getQueueSize();

    public abstract boolean getDiscardOldest();
}
