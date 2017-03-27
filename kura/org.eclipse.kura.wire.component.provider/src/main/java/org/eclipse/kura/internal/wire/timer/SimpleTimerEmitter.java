package org.eclipse.kura.internal.wire.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.wire.WireSupport;

public class SimpleTimerEmitter extends TimerEmitter {

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public SimpleTimerEmitter(WireSupport wireSupport, long interval) {
        super(wireSupport);
        executor.scheduleAtFixedRate(this::emit, 0, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
