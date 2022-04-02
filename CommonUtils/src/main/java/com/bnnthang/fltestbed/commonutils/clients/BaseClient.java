package com.bnnthang.fltestbed.commonutils.clients;

import com.bnnthang.fltestbed.commonutils.enums.ClientCommandEnum;
import com.bnnthang.fltestbed.commonutils.utils.SocketUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.rmi.UnexpectedException;

/**
 * Simple implementation of a Federated Learning client.
 */
public class BaseClient extends Thread {
    private static final Logger _logger = LogManager.getLogger(BaseClient.class);

    /**
     * Delay interval (in milliseconds).
     */
    private Integer delayInterval;

    /**
     * Socket connection to server.
     */
    private Socket socket;

    /**
     * Supported client operations.
     */
    private IClientOperations operations;

    /**
     * Constructor for <code>BaseClient</code>.
     * @param host address to connect
     * @param port port to connect
     * @param _delayInterval delay interval (in milliseconds)
     * @param clientOperations implementation of supported operations
     * @throws IOException if errors happened when initiating the socket
     */
    public BaseClient(final String host,
                      final Integer port,
                      final Integer _delayInterval,
                      final IClientOperations clientOperations)
            throws IOException {
        socket = new Socket(host, port);
        operations = clientOperations;
        delayInterval = _delayInterval;
    }

    @Override
    public void run() {
        try {
            serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start serving instructions from server.
     * @throws IOException if I/O errors happen
     * @throws InterruptedException if something happens during sleep
     */
    public void serve() throws IOException, InterruptedException {
        // close connection if rejected
        if (!acceptedOrRejected()) {
            return;
        }

        do {
            // skip if there is nothing to read
            if (!SocketUtils.availableToRead(socket)) {
                sleep(delayInterval);
                continue;
            }

            coordinate(SocketUtils.readInteger(socket));
        } while (!socket.isClosed() && socket.isConnected());
    }

    /**
     * Branch processing based on server instruction.
     * @param commandIndex an integer denotes a command enum
     * @throws IllegalArgumentException if receives illegal command index
     * @throws UnsupportedOperationException if receives unexpected command
     * @throws IOException if I/O errors happen
     */
    private void coordinate(final Integer commandIndex) throws
            IllegalArgumentException,
            UnsupportedOperationException,
            IOException {
        if (commandIndex < 0 || commandIndex > ClientCommandEnum.values().length) {
            throw new IllegalArgumentException(String.format("got unexpected command index: %d", commandIndex));
        }

        _logger.debug("recv command = " + commandIndex);

        switch (ClientCommandEnum.values()[commandIndex]) {
            case ACCEPTED:
            case REJECTED:
                throw new UnsupportedOperationException(String.format("received unexpected command: %s", ClientCommandEnum.values()[commandIndex].toString()));
            case MODELPUSH:
                operations.handleModelPush(socket);
                break;
            case DATASETPUSH:
                operations.handleDatasetPush(socket);
                break;
            case TRAIN:
                operations.handleTrain();
                break;
            case ISTRAINING:
                operations.handleIsTraining(socket);
                break;
            case REPORT:
                operations.handleReport(socket);
                break;
            case DONE:
                operations.handleDone(socket);
                break;
            default:
                throw new UnsupportedOperationException(String.format("received unrecognized command index: %d", commandIndex));
        }
    }

    /**
     * Check if server rejects connection.
     * @return <code>true</code> iff client is moved to training queue
     * @throws IOException if I/O errors happen
     * @throws UnexpectedException if reads unexpected bytes
     */
    private Boolean acceptedOrRejected() throws IOException, UnexpectedException {
        switch (SocketUtils.readInteger(socket)) {
            case 0:
                operations.handleAccepted(socket);
                return true;
            case 1:
                operations.handleRejected(socket);
                return false;
            default:
                throw new UnexpectedException("unexpected command index");
        }
    }
}
