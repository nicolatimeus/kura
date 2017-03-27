package org.eclipse.kura.internal.wire.timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;

public abstract class TimerEmitter {

    protected WireSupport wireSupport;

    private static final String PROP = "TIMER";

    public TimerEmitter(WireSupport wireSupport) {
        this.wireSupport = wireSupport;
    }

    protected void emit() {
        List<WireRecord> wireRecords = new ArrayList<>();
        Map<String, TypedValue<?>> properties = new HashMap<>();
        properties.put(PROP, TypedValues.newLongValue(System.currentTimeMillis()));
        WireRecord record = new WireRecord(Collections.unmodifiableMap(properties));
        wireRecords.add(record);
        this.wireSupport.emit(wireRecords);
    }

    public abstract void shutdown();
}
