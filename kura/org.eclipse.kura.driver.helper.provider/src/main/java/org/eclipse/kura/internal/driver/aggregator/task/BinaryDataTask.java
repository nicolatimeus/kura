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

import java.io.IOException;

import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;
import org.eclipse.kura.internal.driver.binary.BinaryData;
import org.eclipse.kura.type.TypedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryDataTask<T> extends DriverBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(BinaryDataTask.class);

    private BinaryData<T> dataType;

    public BinaryDataTask(BinaryData<T> dataType, DriverRecord record, Mode mode, int offset) {
        super(record, offset, offset + dataType.getSize(), mode);
        this.dataType = dataType;
    }

    @Override
    public void run(ToplevelBlockTask parent) throws IOException {
        byte[] buffer = ((BufferProvider) parent).getBuffer();

        if (getMode() == Mode.READ) {
            final T result = dataType.read(buffer, getStart() - parent.getStart());

            logger.debug("Read {} {}: offset: {} result: {}", dataType.getClass().getSimpleName(),
                    dataType.getEndianness().name(), getStart(), result);

            record.setDriverStatus(new DriverStatus(DriverFlag.READ_SUCCESSFUL));
            record.setValue(TypedValues.newTypedValue(result));
            record.setTimestamp(System.currentTimeMillis());
        } else {
            @SuppressWarnings("unchecked")
            T value = (T) record.getValue().getValue();

            logger.debug("Write {} {}: offset: {} value: {}", dataType.getClass().getSimpleName(),
                    dataType.getEndianness().name(), getStart(), value);

            dataType.write(buffer, getStart() - parent.getStart(), value);
        }
    }
}
