
package socketfx;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.*;
import java.util.logging.Logger;

public abstract class GenericSocket implements SocketListener {
    
    private final static Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public int port;
    protected DatagramSocket socketConnection = null;
    private Thread socketReaderThread;
    private int debugFlags;
    
    /**
     * Returns true if the specified debug flag is set.
     * @param flag Debug flag in question
     * @return true if the debug flag 'flag' is set.
     */
    public boolean debugFlagIsSet(int flag) {
        return ((flag & debugFlags) != 0);
    }

    /**
     * Set up a connection in the background.  This method returns no status,
     * however the onClosedStatus(boolean) method will be called when the
     * status of the socket changes, either opened or closed (for whatever
     * reason).
     */
    public void connect() {
        try {
            socketReaderThread = new SocketReaderThread();
            socketReaderThread.start();
        } catch (Exception e) {
            if (debugFlagIsSet(Constants.instance().DEBUG_EXCEPTIONS)) {
                LOGGER.info(e.getMessage());
            }
        }  
    }

    /**
     * Shutdown and close GenericSocket instance in an orderly fashion.
     * As per the Java Socket API, once a Socket has been closed, it is not
     * available for further networking use (i.e. can't be reconnected
     * or rebound) A new Socket needs to be created.
     */
    public void shutdown() {
        close();
    }

    /**
     * Close down the GenericSocket infrastructure.  As per the Java Socket
     * API, once a Socket has been closed, it is not available for
     * further networking use (i.e. can't be reconnected or rebound).
     * A new Socket needs to be created.
     *
     * For certain implementations (e.g. ProviderSocket), the
     * closeAdditionalSockets() method may need to be more than just a
     * null method.
     */
    private void close() {
        try {
            if (socketConnection != null && !socketConnection.isClosed()) {
                socketConnection.close();
            }

            closeAdditionalSockets();
            if (debugFlagIsSet(Constants.instance().DEBUG_STATUS)) {
                LOGGER.info("Connection closed");
            }

            onClosedStatus(true);
        } catch (Exception e){
            LOGGER.info(e.getMessage());
        }
    }

    /**
     * This method is called to close any additional sockets that are
     * internally used.  In some cases (e.g. SocketClient), this method
     * should do nothing.  In others (e.g. SocketServer), this method should
     * close the internal ServerSocket instance.
     */
    protected abstract void closeAdditionalSockets();

    /**
     * Send a message in the form of a String to the socket.  A NEWLINE will
     * automatically be appended to the message.
     *
     * @param msg The String message to send
     */
    public void sendMessage(String msg) {
        try {
            if (debugFlagIsSet(Constants.instance().DEBUG_SEND)) {
                String logMsg = "send> " + msg;
                LOGGER.info(logMsg);
            }
        } catch (Exception e) {
            if (debugFlagIsSet(Constants.instance().DEBUG_EXCEPTIONS)) {
                LOGGER.info(e.getMessage());
            }
        }
    }

    class SocketReaderThread extends Thread {

        @Override
        public void run() {
            if (socketConnection != null && socketConnection.isConnected()) {
                onClosedStatus(false) ;
            }

            try {
                DatagramSocket dsocket = new DatagramSocket(port);
                byte[] buffer = new byte[2048];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    dsocket.receive(packet);

                    String msg = new String(buffer, 0, packet.getLength());
                    onMessage(msg);

                    packet.setLength(buffer.length);
                }
            } catch (Exception e) {
                System.err.println(e);
            } finally {
                close();
            }
        }
    }

    public GenericSocket(int port, int debugFlags) {
        this.port = port;
        this.debugFlags = debugFlags;
    }
}
