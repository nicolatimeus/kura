/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/

package org.eclipse.kura.internal.wire.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.wire.WireSupport;

class SimpleTimerEmitter extends TimerEmitter {

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public SimpleTimerEmitter(WireSupport wireSupport, SimpleSchedulingMode schedulingMode, long interval) {
        super(wireSupport);
        if (schedulingMode == SimpleSchedulingMode.FIXED_RATE) {
            executor.scheduleAtFixedRate(this::emit, 0, interval, TimeUnit.MILLISECONDS);
        } else {
            executor.scheduleWithFixedDelay(this::emit, 0, interval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
