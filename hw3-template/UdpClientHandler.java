import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
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
        } catch (Exception e) {
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

    public String receive(byte[] buf) throws IOException, ClassNotFoundException{
        //Called within run
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        dataSocket.receive(packet);
        ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
        ObjectInput in = new ObjectInputStream(bis);

        Request r = (Request) (in.readObject());

        return processCommand(r);
    }

    public String processCommand(Request r) {
        String response = "ERROR";
        switch (r.operationId) {
            case 0:
                int x = 1;
                break;
            case 1:
                bookServer.inventory.put(r.title, bookServer.inventory.get(r.title) - 1);
                int y = bookServer.loanNumber.intValue() + 1;
                // Return message
                break;
            case 2:
                int a = 2;
                break;
            case 3:
                int b = 3;
                break;
            case 4:
                response = bookServer.checkInventory();
                break;
            case 5:
                int c = 4;
                break;
        }
        return response;
    }
}
