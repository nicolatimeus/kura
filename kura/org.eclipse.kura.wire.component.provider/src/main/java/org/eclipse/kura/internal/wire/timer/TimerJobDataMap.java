/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  
 *******************************************************************************/
package org.eclipse.kura.internal.wire.timer;

import org.quartz.JobDataMap;

/**
 * The Class TimerJobDataMap can be used to provide custom Wire Support
 * instances for different Emit Jobs
 */
public final class TimerJobDataMap extends JobDataMap {

    private static final long serialVersionUID = -2191522128203525408L;

    private static final String TIMER_EMITTER = "T";

    public TimerEmitter getTimerEmitter() {
        return (TimerEmitter) super.get(TIMER_EMITTER);
    }

    public void putTimerEmitter(final TimerEmitter timerEmitter) {
        super.put(TIMER_EMITTER, timerEmitter);
    }
}
