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
