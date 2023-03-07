import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClientHandler implements Runnable {

    Socket socket;
    BookServer bookServer;

    ObjectOutputStream oos;
    ObjectInputStream ois;

    Boolean clientRunning = true;

    public TcpClientHandler(Socket socket, BookServer bookServer) throws IOException{
        this.socket = socket;
        this.bookServer = bookServer;

        ois = new ObjectInputStream(this.socket.getInputStream());
        oos = new ObjectOutputStream(this.socket.getOutputStream());
    }

    public void run() {
        try {
            while(clientRunning) {
                Request r = (Request) ois.readObject();
                String response = processCommand(r);

                oos.writeObject(response);
                oos.flush();
            }
        } catch(Exception e) {
            //
        } finally {
            try {
                if (!clientRunning) {
                    oos.flush();
                    oos.close();
                    ois.close();
                }
            } catch (IOException io) {
                //
            }
        }
    }

    public String processCommand(Request r) throws IOException {
        String response = "ERROR";
        switch (r.operationId) {
            case 0:
                if (r.setUdp) {
                    response = "The communication mode is set to UDP";
                } else {
                    response = "The communication mode is set to TCP";
                }
                break;
            case 1:
                if (!(bookServer.inventory.containsKey(r.title))) {
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
                if (bookServer.loanMap.get(r.loanId) == null) {
                    response = String.format("%d not found, no such borrow record", r.loanId);
                } else {
                    String bookTitle = bookServer.loanMap.get(r.loanId).title;
                    int num = bookServer.inventory.get(bookTitle);

                    bookServer.inventory.put(bookTitle, num + 1);
                    bookServer.loanMap.remove(r.loanId);

                    response = String.format("%d is returned", r.loanId);
                }
                break;
            case 3:
                response = "";
                for (int loanId : bookServer.loanMap.keySet()) {
                    LoanInfo info = bookServer.loanMap.get(loanId);
                    if (info.username.equals(r.user)) {
                        response += String.format("%d %s\n", loanId, info.title);
                    }
                }
                if (response.length() == 0) {
                    response = String.format("No record found for %s.", r.user);
                } else {
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
                clientRunning = false;
                break;
        }
        return response;
    }
}
