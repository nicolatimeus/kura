/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.asset.provider.test;

import static org.eclipse.kura.channel.ChannelFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.channel.ChannelFlag.WRITE_SUCCESSFUL;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.channel.ChannelConstants;
import org.eclipse.kura.channel.ChannelEvent;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;

/**
 * Stub Driver implementation required for test
 */
public final class StubDriver implements Driver {

    /** flag to check if driver is connected */
    private boolean isConnected;

    /** {@inheritDoc} */
    @Override
    public void connect() throws ConnectionException {
        this.isConnected = true;
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() throws ConnectionException {
        this.isConnected = false;
    }

    /** {@inheritDoc} */
    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new StubChannelDescriptor();
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> read(final List<ChannelRecord> records) throws ConnectionException {
        if (!this.isConnected) {
            this.connect();
        }

        for (final ChannelRecord record : records) {
            final Map<String, Object> driverRecordConf = record.getChannelConfig();
            switch ((DataType) driverRecordConf.get(ChannelConstants.CHANNEL_VALUE_TYPE.value())) {
            case BOOLEAN:
                record.setValue(TypedValues.newBooleanValue(true));
                break;
            case SHORT:
                record.setValue(TypedValues.newShortValue((short) 1));
                break;
            case LONG:
                record.setValue(TypedValues.newLongValue(1L));
                break;
            case BYTE:
                record.setValue(TypedValues.newByteValue((byte) 1));
                break;
            case BYTE_ARRAY:
                record.setValue(TypedValues.newByteArrayValue("dummy".getBytes()));
                break;
            case DOUBLE:
                record.setValue(TypedValues.newDoubleValue(1.0));
                break;
            case STRING:
                record.setValue(TypedValues.newStringValue("dummy"));
                break;
            case INTEGER:
                record.setValue(TypedValues.newIntegerValue(1));
                break;
            default:
                break;
            }
            record.setChannelStatus(new ChannelStatus(READ_SUCCESSFUL, null, null));
        }
        return records;
    }

    /** {@inheritDoc} */
    @Override
    public void registerDriverListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {
        final ChannelRecord record = new ChannelRecord();
        record.setChannelConfig(channelConfig);
        record.setValue(TypedValues.newIntegerValue(1));
        record.setChannelStatus(new ChannelStatus(READ_SUCCESSFUL, null, null));
        record.setTimestamp(System.currentTimeMillis());
        listener.onDriverEvent(new ChannelEvent(record));
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterDriverListener(final ChannelListener listener) throws ConnectionException {
        // not used
    }

    /** {@inheritDoc} */
    @Override
    public List<ChannelRecord> write(final List<ChannelRecord> records) throws ConnectionException {
        if (!this.isConnected) {
            this.connect();
        }

        for (final ChannelRecord record : records) {
            record.setChannelStatus(new ChannelStatus(WRITE_SUCCESSFUL, null, null));
        }
        return records;
    }

}
