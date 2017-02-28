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

import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;

public abstract class UpdateBlockTask extends DriverBlockTask {

    private ToplevelBlockTask preflightRead;

    public UpdateBlockTask(DriverRecord record, int start, int end, Mode mode) {
        super(record, start, end, mode);
    }

    protected abstract void runRead(ToplevelBlockTask parent);

    protected abstract void runWrite(ToplevelBlockTask parent);

    protected abstract void runUpdate(ToplevelBlockTask parent, ToplevelBlockTask preflightRead);

    @Override
    public void run(ToplevelBlockTask parent) {
        if (getMode() == Mode.READ) {
            runRead(parent);
        } else if (getMode() == Mode.WRITE) {
            runWrite(parent);
        } else {
            if (parent.getMode() == Mode.READ) {
                this.preflightRead = parent;
                return;
            }

            if (preflightRead == null) {
                parent.abort("UPDATE requested but preflight read did not succeed, operation aboreted");
                return;
            }

            runUpdate(parent, preflightRead);
            preflightRead = null;
        }
    }
}
