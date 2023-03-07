import java.io.*;
import java.net.*;
import java.util.Scanner;


public class UdpClientHandler implements Runnable{

    BookServer bookServer;

    DatagramSocket socket;
    DatagramPacket packet;

    public UdpClientHandler(DatagramSocket socket, BookServer bookServer, DatagramPacket packet) throws IOException{
        this.socket = new DatagramSocket();
        this.packet = packet;
        this.bookServer = bookServer;
    }

    public void run() {
        try {
            Request r = receive(packet);
            String response = processCommand(r);
            send(response);
        } catch (Exception e) {
            e.printStackTrace(System.out);
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
                if(r.setUdp) {
                    response = "The communication mode is set to UDP";
                } else {
                    response = "The communication mode is set to TCP";
                }
                break;
            case 1:
                if(!(bookServer.inventory.containsKey(r.title))) {
                    response = "Request Failed - We do not have this book";
                    break;
                }
                int numBooks = bookServer.inventory.get(r.title);
                if (numBooks <= 0) {
                    response = "Request Failed - Book not available";
                    break;
                }

                bookServer.inventory.put(r.title, numBooks - 1);
                int newLoanNum = bookServer.loanNumber.incrementAndGet();

                LoanInfo myInfo = new LoanInfo(r.title, r.user);

                bookServer.loanMap.put(newLoanNum, myInfo);
                response = String.format("Your request has been approved, %d, %s %s",
                                                newLoanNum, r.user, r.title);
                break;
            case 2:
                System.out.println(r.loanId);
                if(bookServer.loanMap.get(r.loanId) == null) {
                    response = String.format("%d not found, no such borrow record", r.loanId);
                }
                else {
                    String bookTitle = bookServer.loanMap.get(r.loanId).title;
                    int num = bookServer.inventory.get(bookTitle);
    
                    bookServer.inventory.put(bookTitle, num + 1);
                    bookServer.loanMap.remove(r.loanId);
    
                    response = String.format("%d is returned", r.loanId);
                } break;
            case 3:
                response = "";
                for (int loanId: bookServer.loanMap.keySet()) {
                    LoanInfo info = bookServer.loanMap.get(loanId);
                    if (info.username.equals(r.user)) {
                        response += String.format("%d %s\n", loanId, info.title);
                    }
                }
                if (response.length() == 0) {
                    response = String.format("No record found for %s.", r.user);
                }
                else {
                    response = response.trim();
                }
                break;
            case 4:
                response = bookServer.checkInventory();
                break;
            case 5:
                FileWriter fileWriter = new FileWriter(new File("inventory.txt"), false);
                fileWriter.write(bookServer.checkInventory());
                fileWriter.close();
                response = "Closing connection with client";
                break;
        }
        return response;
    }

}
