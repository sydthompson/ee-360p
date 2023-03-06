import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;


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

    public String processCommand(Request r) throws IOException {
        String response = "ERROR";
        switch (r.operationId) {
            case 0:
                int x = 1;
                if(r.setUdp) {
                    response = "The communication mode is set to UDP";
                } else {
                    response = "The communication mode is set to TCP";
                }
                break;
            case 1:
                int numBooks = bookServer.inventory.get(r.title);
                if(!bookServer.inventory.contains(r.title) || numBooks <= 0) {
                    response = "Request failed - We do not have this book";
                }
                else {
                    bookServer.inventory.put(r.title, numBooks - 1);
                    int y = bookServer.loanNumber.incrementAndGet();
                   
                    LoanInfo myInfo = new LoanInfo(r.title, r.user);

                    bookServer.loanMap.put(y, myInfo);
                    response = String.format("Your request has been approved, %d, %s %s", 
                                                    y, r.user, r.title);
                } break;
            case 2:
                int a = 2;
                if(!bookServer.loanMap.contains(Integer.parseInt(r.loanId))) {
                    response = String.format("%s not found, no such borrow record", r.loanId);
                }
                else {
                    String bookTitle = bookServer.loanMap.get(Integer.parseInt(r.loanId)).title;
                    int y = bookServer.inventory.get(bookTitle);

                    bookServer.inventory.put(bookTitle, y + 1);
                    bookServer.loanMap.remove(Integer.parseInt(r.loanId));

                    response = String.format("%s is returned", r.loanId);
                } break;
            case 3:
                int b = 3;
                response = "";
                for(int loanId: bookServer.loanMap.keySet()) {
                    LoanInfo info = bookServer.loanMap.get(loanId);
                    if (info.username.equals(r.user)) {
                        response += String.format("%d %s\n", loanId, info.title);
                    }
                }
                if(response.length() == 0) { response = String.format("No record found for %s.", r.user); }
                break;
            case 4:
                response = bookServer.checkInventory();
                break;
            case 5:
                int c = 4;
                //to-do: inform server to stop processing commands from this client
                FileWriter fileWriter = new FileWriter("inventory.txt", false);
                fileWriter.write(bookServer.checkInventory());
                fileWriter.close();
                response = "Closing connection with client";
                break;
        }
        return response;
    }

}
