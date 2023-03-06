import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public class UdpClientHandler implements Runnable{

    DatagramSocket socket;
    BookServer bookServer;
    DatagramSocket dataSocket;

    public UdpClientHandler(DatagramSocket socket, BookServer bookServer) {
        this.socket = socket;
        this.bookServer = bookServer;
        try {
            this.dataSocket = new DatagramSocket(socket.getPort());
        } catch (SocketException e) {
            System.err.println(e);
        } 

    }

    public void run() {
        
        //TODO: Accept packets forever here
        int len = 1024;
        try {
            byte[] buf = new byte[len];
            while(true) {
                String mssg = receive(buf);
                send(mssg);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void send(String message) throws IOException{
        //Called within run
        byte[] buffer = new byte[message.length()];
        buffer = message.getBytes();

        DatagramPacket response = new DatagramPacket(buffer, 
                                                     buffer.length, 
                                                     socket.getInetAddress(), 
                                                     socket.getPort());
        dataSocket.send(response);
    }

    public String receive(byte[] buf) throws IOException{
        //Called within run
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        dataSocket.receive(packet);
        String command = new String(packet.getData(), 0, packet.getLength());
        return bookServer.processCommand(command);
    }
}
