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
 *  Amit Kumar Mondal
 *
 *******************************************************************************/

package org.eclipse.kura.internal.wire.timer;

import static java.util.Objects.nonNull;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.wire.WireSupport;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CronTimerEmitter extends TimerEmitter {

    private static final Logger logger = LoggerFactory.getLogger(CronTimerEmitter.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Group Identifier for Quartz Job and Triggers */
    private static final String GROUP_ID = "wires";

    /** This is required to generate unique ID for the Quartz Trigger and Job */
    private static int id = 0;

    /** Job Key for Quartz Scheduling */
    private JobKey jobKey;

    private Scheduler scheduler;

    public CronTimerEmitter(WireSupport wireSupport, String cronExpression) throws SchedulerException {
        super(wireSupport);
        this.scheduler = createScheduler();
        scheduleCronInterval(cronExpression);
    }

    /**
     * Creates a cron trigger based on the provided interval
     *
     * @param expression
     *            the CRON expression
     * @throws SchedulerException
     *             if scheduling fails
     * @throws NullPointerException
     *             if the argument is null
     */
    private void scheduleCronInterval(final String expression) throws SchedulerException {
        // requireNonNull(expression, message.cronExpressionNonNull());
        ++id;
        if (nonNull(this.jobKey)) {
            this.scheduler.deleteJob(this.jobKey);
        }
        this.jobKey = new JobKey("emitJob" + id, GROUP_ID);
        final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
                .withSchedule(CronScheduleBuilder.cronSchedule(expression)).build();

        final TimerJobDataMap jobDataMap = new TimerJobDataMap();
        jobDataMap.putTimerEmitter(this);
        final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.jobKey).setJobData(jobDataMap).build();

        this.scheduler.getContext().put("wireSupport", this.wireSupport);
        this.scheduler.start();

        this.scheduler.scheduleJob(job, trigger);
    }

    protected Scheduler createScheduler() throws SchedulerException {
        return new StdSchedulerFactory().getScheduler();
    }

    /**
     * This is not a good practice though but in case of Timer, it is very much
     * needed because while deactivating, we cannot just stop the scheduler.
     * Scheduler is a singleton instance shared by all the different instances
     * of Timer and if one Timer is explicitly stopped, all the other Timer
     * instances will be affected. So, it is better to have it dereferenced
     * while finalizing its all references. Even though it is not guaranteed
     * that the reference will be garbage collected at a certain point of time,
     * it is an advise to use it as it is better late than never.
     */
    @Override
    protected void finalize() throws Throwable {
        if (nonNull(this.scheduler)) {
            this.scheduler.shutdown();
            this.scheduler = null;
        }
    }

    @Override
    public void shutdown() {
        if (nonNull(this.jobKey)) {
            try {
                this.scheduler.deleteJob(this.jobKey);
            } catch (final SchedulerException e) {
                logger.error(message.schedulerException(), e);
            }
        }
    }

}
