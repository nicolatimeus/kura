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

package org.eclipse.kura.internal.driver.s7plc.task;

import java.io.IOException;

import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;
import org.eclipse.kura.internal.driver.aggregator.task.BufferProvider;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S7PlcToplevelBlockTask extends ToplevelBlockTask implements BufferProvider {

    private static final Logger logger = LoggerFactory.getLogger(S7PlcDriver.class);

    private int areaNo;
    private byte[] data;
    private S7PlcDriver driver;
    private boolean isValid;

    S7PlcToplevelBlockTask(S7PlcDriver driver, Mode mode, int dbNumber, int start, int end) {
        super(start, end, mode);
        this.areaNo = dbNumber;
        this.driver = driver;
        this.data = new byte[end - start];
    }

    @Override
    public void run(ToplevelBlockTask parent) throws IOException {
        if (getMode() == Mode.READ) {
            isValid = false;
            logger.debug("Reading from PLC, DB{} offset: {} length: {}", areaNo, getStart(), data.length);
            driver.read(areaNo, getStart(), data);
            isValid = true;
            runChildren();
        } else {
            isValid = true;
            runChildren();
            logger.debug("Writing to PLC, DB{} offset: {} length: {}", areaNo, getStart(), data.length);
            driver.write(areaNo, getStart(), data);
            succeedChildren();
        }
    }

    @Override
    public byte[] getBuffer() {
        return data;
    }

    @Override
    public void fail(ToplevelBlockTask parent, String reason) {
        failChildren(reason);
    }

    @Override
    public void succeed(ToplevelBlockTask parent) {
    }

    @Override
    public boolean isValid() {
        return isValid;
    }
}
