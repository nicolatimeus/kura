/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.internal.driver.aggregator.task;

import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.internal.driver.aggregator.BlockTask;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;

public abstract class DriverBlockTask extends BlockTask {

    protected DriverRecord record;

    public DriverBlockTask(DriverRecord record, int start, int end, Mode mode) {
        super(start, end, mode);
        this.record = record;
    }

    @Override
    public void succeed(ToplevelBlockTask parent) {
        record.setDriverStatus(
                new DriverStatus(getMode() == Mode.READ ? DriverFlag.READ_SUCCESSFUL : DriverFlag.WRITE_SUCCESSFUL));
        record.setTimestamp(System.currentTimeMillis());
    }

    @Override
    public void fail(ToplevelBlockTask parent, String reason) {
        record.setDriverStatus(new DriverStatus(
                getMode() == Mode.READ ? DriverFlag.READ_FAILURE : DriverFlag.WRITE_FAILURE, reason, null));
        record.setTimestamp(System.currentTimeMillis());
    }
}
