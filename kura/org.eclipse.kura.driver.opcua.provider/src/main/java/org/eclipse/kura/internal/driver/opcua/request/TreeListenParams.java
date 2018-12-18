package org.eclipse.kura.internal.driver.opcua.request;

import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class TreeListenParams extends SingleNodeListenParams {

    public TreeListenParams(Map<String, Object> channelConfig) {
        super(channelConfig);
    }

    public TreeListenParams(final ReadValueId readValueId, final double samplingInterval, final long queueSize,
            final boolean discardOldest) {
        super(readValueId, samplingInterval, queueSize, discardOldest);
    }
}
