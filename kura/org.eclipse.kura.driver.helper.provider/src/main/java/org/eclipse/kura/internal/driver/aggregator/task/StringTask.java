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

import java.nio.charset.StandardCharsets;

import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;
import org.eclipse.kura.type.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringTask extends DriverBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(StringTask.class);

    public StringTask(DriverRecord record, int start, int end, Mode mode) {
        super(record, start, end, mode);
    }

    @Override
    public void run(ToplevelBlockTask p) {
        byte[] buffer = ((BufferProvider) p).getBuffer();

        if (getMode() == Mode.READ) {
            byte[] data = new byte[getEnd() - getStart()];
            System.arraycopy(buffer, getStart() - p.getStart(), data, 0, data.length);

            final String result = new String(data, StandardCharsets.US_ASCII);

            logger.debug("Read string: offset: {} length: {} result: {}", getStart(), data.length, result);

            record.setDriverStatus(new DriverStatus(DriverFlag.READ_SUCCESSFUL));
            record.setValue(new StringValue(result));
            record.setTimestamp(System.currentTimeMillis());
        } else {
            String value = (String) record.getValue().getValue();
            int writeLength = Math.min(getEnd() - getStart(), value.length());

            logger.debug("Write string: offset: {} length: {} value: {}", getStart(), writeLength, value);

            final byte[] data = value.getBytes(StandardCharsets.US_ASCII);

            System.arraycopy(data, 0, buffer, getStart() - p.getStart(), writeLength);
        }
    }
}