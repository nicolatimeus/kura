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

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Timer represents a Wire Component which triggers a ticking event on
 * every interval as configured. It fires the event on every tick.
 */
public class Timer implements WireEmitter, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(Timer.class);

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private TimerEmitter emitter;

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private TimerOptions timerOptions;

    private static void debug(Supplier<String> message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message.get());
        }
    }

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi service component activation callback
     *
     * @param ctx
     *            the component context
     * @param properties
     *            the configured properties
     */
    protected void activate(final ComponentContext ctx, final Map<String, Object> properties) {
        debug(() -> message.activatingTimer());
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.updated(properties);
        debug(() -> message.activatingTimerDone());
    }

    /**
     * OSGi service component modification callback
     *
     * @param properties
     *            the updated properties
     */
    protected void updated(final Map<String, Object> properties) {
        debug(() -> message.updatingTimer());
        this.timerOptions = new TimerOptions(properties);
        try {
            initEmitter();
        } catch (final SchedulerException e) {
            logger.error(message.schedulerException(), e);
        }
        debug(() -> message.updatingTimerDone());
    }

    /**
     * OSGi service component deactivation callback
     *
     * @param ctx
     *            the component context
     */
    protected void deactivate(final ComponentContext ctx) {
        debug(() -> message.deactivatingTimer());
        shutdownEmitter();
        debug(() -> message.deactivatingTimerDone());
    }

    private void initEmitter() throws SchedulerException {
        shutdownEmitter();
        if (timerOptions.getType().equals("SIMPLE")) {
            this.emitter = new SimpleTimerEmitter(wireSupport, timerOptions.getSimpleSchedulingMode(),
                    timerOptions.getSimpleInterval() * timerOptions.getSimpleTimeUnitMultiplier());
        } else {
            this.emitter = new CronTimerEmitter(wireSupport, timerOptions.getCronExpression());
        }
    }

    private void shutdownEmitter() {
        if (emitter != null) {
            emitter.shutdown();
        }
        emitter = null;
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

}
