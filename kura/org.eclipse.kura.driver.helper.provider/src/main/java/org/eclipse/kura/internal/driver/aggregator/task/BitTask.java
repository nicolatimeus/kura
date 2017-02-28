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
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;
import org.eclipse.kura.type.BooleanValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitTask extends UpdateBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(BitTask.class);
    private int bit;

    public BitTask(DriverRecord record, int start, int bit, Mode mode) {
        super(record, start, start + 1, mode);
        this.bit = bit;
    }

    public void setBit(int bit) {
        this.bit = bit;
    }

    @Override
    protected void runRead(ToplevelBlockTask parent) {
        byte[] buffer = ((BufferProvider) parent).getBuffer();

        byte b = buffer[getStart() - parent.getStart()];

        final boolean result = ((b >> bit) & 0x01) == 1;

        logger.debug("Reading Bit: offset {} bit index {} result {}", getStart(), bit, result);

        record.setDriverStatus(new DriverStatus(DriverFlag.READ_SUCCESSFUL));
        record.setValue(new BooleanValue(result));
        record.setTimestamp(System.currentTimeMillis());
    }

    @Override
    protected void runWrite(ToplevelBlockTask parent) {
        logger.warn("Write mode not supported");
        fail(parent, "BitTask does not support WRITE mode, only READ and UPDATE modes are supported");
    }

    @Override
    protected void runUpdate(ToplevelBlockTask parent, ToplevelBlockTask preflightRead) {
        byte[] outBuffer = ((BufferProvider) parent).getBuffer();
        byte[] inBuffer = ((BufferProvider) preflightRead).getBuffer();

        final int previousValueOffset = getStart() - preflightRead.getStart();
        final boolean value = (Boolean) record.getValue().getValue();

        byte byteValue = inBuffer[previousValueOffset];

        if (value) {
            byteValue |= 1 << bit;
        } else {
            byteValue &= ~(1 << bit);
        }

        inBuffer[previousValueOffset] = byteValue;
        logger.debug("Write Bit: offset: {} value: {}", getStart(), value);
        outBuffer[getStart() - parent.getStart()] = byteValue;
    }
}