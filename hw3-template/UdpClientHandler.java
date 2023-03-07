import java.io.*;
import java.net.*;
import java.util.Scanner;


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
            send(response);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public void send(String message) throws IOException{
        //Called within run
        DatagramPacket response = new DatagramPacket(message.getBytes(),
                                                     message.length(),
                                                     packet.getAddress(),
                                                     packet.getPort());
        socket.send(response);
    }

    public Request receive(DatagramPacket packet) throws IOException, ClassNotFoundException{
        ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
        ObjectInputStream in = new ObjectInputStream(bais);
        Request r = (Request) (in.readObject());

        return r;
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
                if(!(bookServer.inventory.get(r.title) == null) || numBooks <= 0) {
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
                if(bookServer.loanMap.get(Integer.parseInt(r.loanId)) == null) {
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
                FileWriter fileWriter = new FileWriter(new File("inventory.txt"), false);
                fileWriter.write(bookServer.checkInventory());
                fileWriter.close();
                response = "Closing connection with client";
                break;
        }
        return response;
    }

}
