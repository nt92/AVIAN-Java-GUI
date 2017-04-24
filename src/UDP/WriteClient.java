package UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

class WriteClient {
    public static void main(String args[]) throws Exception {
        int clientPort = 4222;

        int buffer_size = 1024;

        byte buffer[] = new byte[buffer_size];
        DatagramSocket ds = new DatagramSocket(clientPort);
        while (true) {
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            ds.receive(p);
            System.out.println(new String(p.getData(), 0, p.getLength()));
        }
    }
}