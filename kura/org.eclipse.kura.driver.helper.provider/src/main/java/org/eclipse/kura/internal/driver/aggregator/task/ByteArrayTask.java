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
import org.eclipse.kura.type.ByteArrayValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteArrayTask extends DriverBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(ByteArrayTask.class);

    public ByteArrayTask(DriverRecord record, Mode mode, int start, int end) {
        super(record, start, end, mode);
    }

    @Override
    public void run(ToplevelBlockTask p) {
        byte[] buffer = ((BufferProvider) p).getBuffer();

        if (getMode() == Mode.READ) {
            byte[] data = new byte[getEnd() - getStart()];

            System.arraycopy(buffer, getStart() - p.getStart(), data, 0, data.length);
            logger.debug("Read byte array: offset: {} length: {} result: {}", getStart(), data.length, data);

            record.setDriverStatus(new DriverStatus(DriverFlag.READ_SUCCESSFUL));
            record.setValue(new ByteArrayValue(data));
            record.setTimestamp(System.currentTimeMillis());
        } else {
            byte[] value = (byte[]) record.getValue().getValue();
            int writeLength = Math.min(getEnd() - getStart(), value.length);

            logger.debug("Write byte array: offset: {} length: {} value: {}", getStart(), writeLength, value);

            System.arraycopy(value, 0, buffer, getStart() - p.getStart(), writeLength);
        }
    }
}
