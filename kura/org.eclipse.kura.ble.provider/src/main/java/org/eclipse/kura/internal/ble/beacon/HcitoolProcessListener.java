package org.eclipse.kura.internal.ble.beacon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.ble.util.BluetoothProcessListener;

public class HcitoolProcessListener implements BluetoothProcessListener {

    private static final HcitoolProcessListener INSTANCE = new HcitoolProcessListener();

    private HcitoolProcessListener() {
    }

    public static HcitoolProcessListener instance() {
        return INSTANCE;
    }

    private static final String COMMAND_MESSAGE = "Command ";
    private static final Logger logger = LogManager.getLogger(HcitoolProcessListener.class);

    private void parseReturnString(String[] lines) throws KuraBluetoothCommandException {
        String lastLine = lines[lines.length - 1];

        String command = lines[0].substring(15, 35);

        // The last line of hcitool cmd return contains:
        // the numbers of packets sent (1 byte)
        // the opcode (2 bytes)
        // the exit code (1 byte)
        // the returned data if any
        String exitCode = lastLine.substring(11, 13);

        switch (exitCode.toLowerCase()) {
        case "00":
            logger.debug("Command {} Succeeded.", command);
            break;
        case "01":
            // The Unknown HCI Command error code indicates that the Controller does not understand the HCI
            // Command Packet OpCode that the Host sent.
            logger.debug("Command {} failed. Error: Unknown HCI Command (01)", command);
            throw new KuraBluetoothCommandException(
                    COMMAND_MESSAGE + command + " failed. Error: Unknown HCI Command (01)");
        case "03":
            // The Hardware Failure error code indicates to the Host that something in the Controller has failed
            // in a manner that cannot be described with any other error code.
            logger.debug("Command {} failed. Error: Hardware Failure (03)", command);
            throw new KuraBluetoothCommandException(
                    COMMAND_MESSAGE + command + " failed. Error: Hardware Failure (03)");
        case "0c":
            // The Command Disallowed error code indicates that the command requested cannot be executed because
            // the Controller is in a state where it cannot process this command at this time. This error code is
            // usually used when a command is run twice, so no exception is to be thrown, here.
            logger.debug("Command {} failed. Error: Command Disallowed (0C)", command);
            break;
        case "11":
            // The Unsupported Feature Or Parameter Value error code indicates that a feature or parameter value
            // in the HCI command is not supported.
            logger.debug("Command {} failed. Error: Unsupported Feature or Parameter Value (11)", command);
            throw new KuraBluetoothCommandException(
                    COMMAND_MESSAGE + command + " failed. Unsupported Feature or Parameter Value (11)");
        case "12":
            // The Invalid HCI Command Parameters error code indicates that at least one of the HCI command
            // parameters is invalid.
            logger.debug("Command {} failed. Error: Invalid HCI Command Parameters (12)", command);
            throw new KuraBluetoothCommandException(
                    COMMAND_MESSAGE + command + " failed. Error: Invalid HCI Command Parameters (12)");
        default:
            logger.debug("Command {} failed. Error {}", command, exitCode);
            throw new KuraBluetoothCommandException(COMMAND_MESSAGE + command + " failed. Error " + exitCode);
        }
    }

    @Override
    public void processInputStream(String string) throws KuraException {
        logger.debug("Command response : {}", string);
        String[] lines = string.split("\n");
        if (!string.isEmpty() && lines.length >= 1) {
            if (lines[0].toLowerCase().contains("unknown")
                    || lines.length >= 2 && lines[1].toLowerCase().contains("usage")) {
                throw new KuraBluetoothCommandException("Command failed. Error in command syntax.");
            } else if (lines[0].toLowerCase().contains("invalid") || lines[0].toLowerCase().contains("error")) {
                throw new KuraBluetoothCommandException("Command failed.");
            } else {
                parseReturnString(lines);
            }
        }
    }

    @Override
    public void processInputStream(int ch) throws KuraException {
        // Not used
    }

    @Override
    public void processErrorStream(String string) throws KuraException {
        // Not used
    }
}
