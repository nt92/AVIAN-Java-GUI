package socketfx;

import java.lang.invoke.MethodHandles;
import java.net.*;
import java.util.logging.Logger;

public abstract class GenericSocket implements SocketListener {
    
    private final static Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public int port;
    protected DatagramSocket socketConnection = null;
    private Thread socketReaderThread;
    private Thread socketWriterThread;

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
            LOGGER.info(e.getMessage());
        }  
    }

    public void send(String host, String port, String s){
        try {
            socketWriterThread = new SocketWriterThread(s);
            socketWriterThread.start();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
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

            LOGGER.info("Connection closed");


            onClosedStatus(true);
        } catch (Exception e){
            LOGGER.info(e.getMessage());
        }
    }

    class SocketWriterThread extends Thread {

        String s;

        public SocketWriterThread(String s){
            this.s = s;
        }

        @Override
        public void run() {
            if (socketConnection != null && socketConnection.isConnected()) {
                onClosedStatus(false) ;
            }

            try {
                String host = "localhost";

                byte[] message = s.getBytes();

                // Get the internet address of the specified host
                InetAddress address = InetAddress.getByName(host);

                // Initialize a datagram packet with data and address
                DatagramPacket packet = new DatagramPacket(message, message.length,
                        address, port);

                // Create a datagram socket, send the packet through it, close it.
                DatagramSocket dsocket = new DatagramSocket();
                dsocket.send(packet);
                dsocket.close();
            } catch (Exception e) {
                System.err.println(e);
            } finally {
//                System.out.println("Finished Writing");
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
                socketConnection = new DatagramSocket(port);
                byte[] buffer = new byte[2048];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    socketConnection.receive(packet);

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

    public GenericSocket(int port) {
        this.port = port;
    }
}
