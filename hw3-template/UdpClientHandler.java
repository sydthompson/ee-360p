import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public class UdpClientHandler implements Runnable{

    BookServer bookServer;

    DatagramSocket socket;
    DatagramPacket packet;

    public UdpClientHandler(DatagramSocket socket, BookServer bookServer, DatagramPacket packet) throws IOException{
        this.socket = new DatagramSocket();
        System.out.println(bookServer.udpPort);
        this.packet = packet;
        this.bookServer = bookServer;
    }

    public void run() {
        try {
            Request r = receive(packet);
            String response = processCommand(r);
            System.out.println(response);
            send(response);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void send(String message) throws IOException{
        //Called within run
        byte[] buffer = getByteArray(message);
        DatagramPacket response = new DatagramPacket(buffer, 
                                                     buffer.length, 
                                                     socket.getInetAddress(),
                                                     bookServer.udpPort);
        System.out.println("Sending");
        socket.send(response);
    }

    private byte[] getByteArray(String message) throws IOException{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream write = new ObjectOutputStream(stream);
        write.writeObject(message);
        write.flush();
        return stream.toByteArray();
    }

    public Request receive(DatagramPacket packet) throws IOException, ClassNotFoundException{
        ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
        ObjectInputStream in = new ObjectInputStream(bais);
        Request r = (Request) (in.readObject());

        return r;
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
