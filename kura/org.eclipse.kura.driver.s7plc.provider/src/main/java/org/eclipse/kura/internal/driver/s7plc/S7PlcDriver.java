/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.s7plc;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.driver.s7plc.localization.S7PlcMessages;
import org.eclipse.kura.internal.driver.aggregator.BlockTask;
import org.eclipse.kura.internal.driver.aggregator.BlockTaskOptimizer;
import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.task.DriverBlockTask;
import org.eclipse.kura.internal.driver.s7plc.task.S7PlcTaskBuilder;
import org.eclipse.kura.internal.driver.s7plc.task.S7PlcToplevelBlockFactory;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Moka7.S7;
import Moka7.S7Client;

/**
 * The Class S7PlcDriver is a S7 PLC Driver implementation for Kura Asset-Driver
 * Topology.<br/>
 * <br/>
 *
 * This S7 PLC Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required properties are enlisted in {@link S7PlcChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link S7PlcOptions}
 *
 * @see S7PlcChannelDescriptor
 * @see S7PlcOptions
 */
public final class S7PlcDriver implements Driver {

    private static final Logger logger = LoggerFactory.getLogger(S7PlcDriver.class);

    private static final S7PlcMessages messages = LocalizationAdapter.adapt(S7PlcMessages.class);

    private S7Client client = new S7Client();

    private S7PlcOptions options;

    /**
     * OSGi service component callback while activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(messages.activating());
        this.extractProperties(properties);
        logger.debug(messages.activatingDone());
    }

    private void authenticate() throws ConnectionException {
        logger.debug(messages.authenticating());
        int code = this.client.SetSessionPassword(this.options.getPassword());
        if (code != 0) {
            throw new ConnectionException(messages.errorAuthenticating() + code);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void connect() throws ConnectionException {
        try {
            if (!this.client.Connected) {
                logger.debug(messages.connecting());
                client.SetConnectionType(S7.OP);
                int code = this.client.ConnectTo(this.options.getIp(), this.options.getRack(), this.options.getSlot());
                if (code != 0) {
                    throw new ConnectionException(messages.errorConnectToFailed() + code);
                }
                if (this.options.shouldAuthenticate()) {
                    authenticate();
                }
                logger.debug(messages.connectingDone());
            }
        } catch (Exception e) {
            throw new ConnectionException(messages.errorUnexpectedConnectionException(), e);
        }
    }

    /**
     * OSGi service component callback while deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(messages.deactivating());
        try {
            this.disconnect();
        } catch (final ConnectionException e) {
            logger.error(messages.errorDisconnecting(), e);
        }
        logger.debug(messages.deactivatingDone());
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() throws ConnectionException {
        if (this.client.Connected) {
            logger.debug(messages.disconnecting());
            this.client.Disconnect();
            logger.debug(messages.disconnectingDone());
        }
    }

    /**
     * Extract the S7 PLC specific configurations from the provided properties.
     *
     * @param properties
     *            the provided properties to parse
     */
    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, messages.propertiesNonNull());
        this.options = new S7PlcOptions(properties);
    }

    /** {@inheritDoc} */
    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new S7PlcChannelDescriptor();
    }

    private List<DriverRecord> run(List<DriverRecord> records, int transferSizeHint, Mode mode) throws KuraException {

        S7PlcTaskBuilder builder = new S7PlcTaskBuilder(mode);
        S7PlcToplevelBlockFactory factory = new S7PlcToplevelBlockFactory(this);

        HashMap<Integer, LinkedList<BlockTask>> tasks = new HashMap<>();
        for (DriverRecord record : records) {
            int areaNo = builder.getAreaNo(record);
            if (areaNo == -1) {
                logger.warn("{}: {}", messages.errorInvalidChannelConfig(), record.getChannelConfig());
                continue;
            }
            DriverBlockTask task = builder.build(record);
            if (task == null) {
                logger.warn("{}: {}", messages.errorInvalidChannelConfig(), record.getChannelConfig());
                continue;
            }
            LinkedList<BlockTask> dbTasks = tasks.get(areaNo);
            if (dbTasks == null) {
                dbTasks = new LinkedList<>();
                tasks.put(areaNo, dbTasks);
            }
            dbTasks.add(task);
        }
        LinkedList<BlockTask> resultTaskList = new LinkedList<>();

        for (Map.Entry<Integer, LinkedList<BlockTask>> e : tasks.entrySet()) {
            factory.setAreaNo(e.getKey());
            BlockTaskOptimizer.optimize(factory, e.getValue(), resultTaskList, (transferSizeHint == 0) ? null
                    : new BlockTaskOptimizer.SetTransferSizeHintCustomizer(transferSizeHint));
        }
        for (BlockTask t : resultTaskList) {
            runTask(t);
        }
        return records;
    }

    private void runTask(BlockTask task) {
        try {
            task.run(null);
        } catch (Moka7Exception e) {
            handleMoka7IOException(e);
            task.fail(null, messages.errorIOFailed() + " " + e.getMessage());
        } catch (Exception e) {
            logger.warn(messages.errorUnexpectedException(), e);
            task.fail(null, messages.errorUnexpectedException() + " " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
        if (!this.client.Connected) {
            this.connect();
        }
        try {
            return run(records, this.options.getTransferSizeHint(), Mode.READ);
        } catch (Exception e) {
            logger.warn(messages.errorUnexpectedException(), e);
            for (DriverRecord record : records) {
                record.setDriverStatus(new DriverStatus(DriverFlag.UNKNOWN, e.getMessage(), e));
                record.setTimestamp(System.currentTimeMillis());
            }
            return records;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
            throws ConnectionException {
        throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterDriverListener(final DriverListener listener) throws ConnectionException {
        throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    /**
     * OSGi service component callback while updating.
     *
     * @param properties
     *            the properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(messages.updating());
        this.extractProperties(properties);
        if (client.Connected) {
            try {
                logger.info(messages.reconnectingAfterConfigurationUpdate());
                disconnect();
                connect();
            } catch (ConnectionException e) {
                logger.warn(messages.errorReconnectFailed(), e);
            }
        }
        logger.debug(messages.updatingDone());
    }

    public void write(int area, int offset, byte[] data) throws IOException {
        int result = this.client.WriteArea(S7.S7AreaDB, area, offset, data.length, data);
        if (result != 0) {
            throw new Moka7Exception(
                    "write failed: DB" + area + " off: " + offset + " len: " + data.length + " status: " + result,
                    result);
        }
    }

    public void read(int area, int offset, byte[] data) throws IOException {
        int result = this.client.ReadArea(S7.S7AreaDB, area, offset, data.length, data);
        if (result != 0) {
            throw new Moka7Exception(
                    "read failed: DB" + area + " off: " + offset + " len: " + data.length + " status: " + result,
                    result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
        if (!this.client.Connected) {
            this.connect();
        }
        try {
            return run(records, 0, Mode.WRITE);
        } catch (Exception e) {
            logger.warn(messages.errorUnexpectedException(), e);
            for (DriverRecord record : records) {
                record.setDriverStatus(new DriverStatus(DriverFlag.UNKNOWN, e.getMessage(), e));
                record.setTimestamp(System.currentTimeMillis());
            }
            return records;
        }
    }

    private void handleMoka7IOException(Moka7Exception e) {
        logger.warn(messages.errorIOFailed(), e);
        if (e.getStatusCode() <= S7Client.errTCPConnectionReset) {
            logger.warn(messages.connectionProblemsDetected());
            try {
                disconnect();
            } catch (ConnectionException e1) {
                logger.warn(messages.disconnectionProblem(), e1);
            }
        }
    }

    private class Moka7Exception extends IOException {

        private int statusCode;

        public Moka7Exception(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
