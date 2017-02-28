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

import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;

import java.util.Map;

import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.s7plc.localization.S7PlcMessages;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.task.BinaryDataTask;
import org.eclipse.kura.internal.driver.aggregator.task.BitTask;
import org.eclipse.kura.internal.driver.aggregator.task.ByteArrayTask;
import org.eclipse.kura.internal.driver.aggregator.task.DriverBlockTask;
import org.eclipse.kura.internal.driver.aggregator.task.StringTask;
import org.eclipse.kura.internal.driver.binary.BinaryData;
import org.eclipse.kura.internal.driver.binary.BinaryDataTypes;
import org.eclipse.kura.internal.driver.binary.Endianness;
import org.eclipse.kura.internal.driver.s7plc.S7PlcChannelDescriptor;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDataType;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.type.DataType;

public class S7PlcTaskBuilder {

    private static final S7PlcMessages messages = LocalizationAdapter.adapt(S7PlcMessages.class);

    private static final BinaryData<Double> REAL_DATA_TYPE_WRAPPER = new RealDataTypeWrapper();
    private static final BinaryData<Integer> INT_DATA_TYPE_WRAPPER = new IntDataTypeWrapper();

    private Mode mode;

    public S7PlcTaskBuilder(Mode mode) {
        this.mode = mode;
    }

    private int getIntProperty(DriverRecord record, String propertyName, DriverFlag failureFlag, String failureMessage)
            throws ValidationException {
        try {
            return Integer.parseInt(record.getChannelConfig().get(propertyName).toString());
        } catch (Exception e) {
            throw new ValidationException(failureFlag, failureMessage);
        }
    }

    private void assertChannelType(DriverRecord record, DataType channelType) throws ValidationException {
        if (channelType != (DataType) record.getChannelConfig().get(CHANNEL_VALUE_TYPE.value())) {
            throw new ValidationException(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
                    messages.errorConvertingType() + channelType);
        }
    }

    private DataType getChannelType(Map<String, Object> channelConfig) throws ValidationException {
        Object type = channelConfig.get(CHANNEL_VALUE_TYPE.value());
        if (type == null) {
            throw new ValidationException(DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
                    CHANNEL_VALUE_TYPE.value() + " property not found in channel config");
        }
        if (!(type instanceof DataType)) {
            throw new ValidationException(DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
                    "value of property" + CHANNEL_VALUE_TYPE.value() + " should be an instance of DataType");
        }
        return (DataType) type;
    }

    private DriverBlockTask buildBitTask(DriverRecord record, int offset) throws ValidationException {

        assertChannelType(record, DataType.BOOLEAN);
        int bitIndex = getIntProperty(record, S7PlcChannelDescriptor.BIT_INDEX_ID, DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
                messages.errorRetrievingBitIndex());
        return new BitTask(record, offset, bitIndex, mode == Mode.WRITE ? Mode.UPDATE : Mode.READ);
    }

    private DriverBlockTask buildByteArrayTask(DriverRecord record, int offset) throws ValidationException {

        assertChannelType(record, DataType.BYTE_ARRAY);
        int byteCount = getIntProperty(record, S7PlcChannelDescriptor.BYTE_COUNT_ID,
                DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, messages.errorRetrievingByteCount());
        return new ByteArrayTask(record, mode, offset, offset + byteCount);
    }

    private DriverBlockTask buildStringTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.STRING);
        int byteCount = getIntProperty(record, S7PlcChannelDescriptor.BYTE_COUNT_ID,
                DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, messages.errorRetrievingByteCount());
        return new StringTask(record, offset, offset + byteCount, mode);
    }

    private DriverBlockTask buildIntTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.INTEGER);
        return new BinaryDataTask<>(INT_DATA_TYPE_WRAPPER, record, mode, offset);
    }

    private DriverBlockTask buildDIntTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.INTEGER);
        return new BinaryDataTask<>(BinaryDataTypes.INT32_BE, record, mode, offset);
    }

    private DriverBlockTask buildWordTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.INTEGER);
        return new BinaryDataTask<>(BinaryDataTypes.UINT16_BE, record, mode, offset);
    }

    private DriverBlockTask buildDWordTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.LONG);
        return new BinaryDataTask<>(BinaryDataTypes.UINT32_BE, record, mode, offset);
    }

    private DriverBlockTask buildByteTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.BYTE);
        return new BinaryDataTask<>(BinaryDataTypes.UINT8, record, mode, offset);
    }

    private DriverBlockTask buildRealTask(DriverRecord record, int offset) throws ValidationException {
        assertChannelType(record, DataType.DOUBLE);
        return new BinaryDataTask<>(REAL_DATA_TYPE_WRAPPER, record, mode, offset);
    }

    public int getAreaNo(DriverRecord record) {
        try {
            return getIntProperty(record, S7PlcChannelDescriptor.DATA_BLOCK_NO_ID,
                    DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION, messages.errorRetrievingAreaNo());
        } catch (ValidationException e) {
            record.setDriverStatus(new DriverStatus(e.getFlag(), e.getMessage(), e));
            record.setTimestamp(System.currentTimeMillis());
            return -1;
        }
    }

    public DriverBlockTask build(DriverRecord record) {

        try {
            final Map<String, Object> channelConfig = record.getChannelConfig();

            DataType type = getChannelType(channelConfig);

            int offset = getIntProperty(record, S7PlcChannelDescriptor.OFFSET_ID,
                    DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION, messages.errorRetrievingAreaOffset());
            String s7DataTypeId = (String) channelConfig.get(S7PlcChannelDescriptor.S7_ELEMENT_TYPE_ID);

            if (type == DataType.BYTE_ARRAY) {

                return buildByteArrayTask(record, offset);

            } else if (S7PlcDataType.INT.name().equals(s7DataTypeId)) {

                return buildIntTask(record, offset);

            } else if (S7PlcDataType.DINT.name().equals(s7DataTypeId)) {

                return buildDIntTask(record, offset);

            } else if (S7PlcDataType.BOOL.name().equals(s7DataTypeId)) {

                return buildBitTask(record, offset);

            } else if (S7PlcDataType.WORD.name().equals(s7DataTypeId)) {

                return buildWordTask(record, offset);

            } else if (S7PlcDataType.DWORD.name().equals(s7DataTypeId)) {

                return buildDWordTask(record, offset);

            } else if (S7PlcDataType.BYTE.name().equals(s7DataTypeId)) {

                return buildByteTask(record, offset);

            } else if (S7PlcDataType.CHAR.name().equals(s7DataTypeId)) {

                return buildStringTask(record, offset);
            } else if (S7PlcDataType.REAL.name().equals(s7DataTypeId)) {

                return buildRealTask(record, offset);
            }

            throw new ValidationException(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, messages.errorUnknownOperation());
        } catch (ValidationException e) {
            record.setDriverStatus(new DriverStatus(e.getFlag(), e.getMessage(), e));
            record.setTimestamp(System.currentTimeMillis());
            return null;
        } catch (Exception e) {
            record.setDriverStatus(new DriverStatus(DriverFlag.UNKNOWN, e.getMessage(), e));
            record.setTimestamp(System.currentTimeMillis());
            return null;
        }
    }

    private class ValidationException extends Exception {

        private DriverFlag flag;

        public ValidationException(DriverFlag flag, String errorMessage) {
            super(errorMessage);
            this.flag = flag;
        }

        public DriverFlag getFlag() {
            return flag;
        }
    }

    private static class RealDataTypeWrapper extends BinaryData<Double> {

        public RealDataTypeWrapper() {
            super(Endianness.BigEndian, BinaryDataTypes.FLOAT_BE.getSize());
        }

        @Override
        public void write(byte[] buf, int offset, Double value) {
            BinaryDataTypes.FLOAT_BE.write(buf, offset, value.floatValue());
        }

        @Override
        public Double read(byte[] buf, int offset) {
            return BinaryDataTypes.FLOAT_BE.read(buf, offset).doubleValue();
        }

    }

    private static class IntDataTypeWrapper extends BinaryData<Integer> {

        public IntDataTypeWrapper() {
            super(Endianness.BigEndian, BinaryDataTypes.INT16_BE.getSize());
        }

        @Override
        public void write(byte[] buf, int offset, Integer value) {
            BinaryDataTypes.INT16_BE.write(buf, offset, value.shortValue());
        }

        @Override
        public Integer read(byte[] buf, int offset) {
            return BinaryDataTypes.INT16_BE.read(buf, offset).intValue();
        }

    }
}
