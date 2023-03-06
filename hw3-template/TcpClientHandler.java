import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClientHandler implements Runnable {

    Socket socket;
    BookServer bookServer;

    ObjectOutputStream oos;
    ObjectInputStream ois;

    public TcpClientHandler(Socket socket, BookServer bookServer) throws IOException{
        this.socket = socket;
        this.bookServer = bookServer;

        ois = new ObjectInputStream(this.socket.getInputStream());
        oos = new ObjectOutputStream(this.socket.getOutputStream());
    }

    public void run() {
        //TODO: Read/write packets here
        try {
            System.out.println("Waiting for requests");
            Request r = (Request) ois.readObject();
            System.out.println(r.operationId);
            String response = processCommand(r);

            oos.writeObject(response);
            oos.flush();
        } catch(Exception e) {
            System.err.println(e);
        }
    }

//    public void send(String mssg) throws IOException{
//        //Called within run
//        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//        out.println(mssg);
//        out.flush();
//
//    }
//
//    public String receive() throws IOException{
//        //Called within run
//        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//        Scanner sc = new Scanner(in);
//        String command = sc.nextLine();
//        sc.close();
//        return response;
//    }

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
