import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClientHandler implements Runnable {

    Socket socket;
    BookServer bookServer;


    public TcpClientHandler(Socket socket, BookServer bookServer)  {
        this.socket = socket;
        this.bookServer = bookServer;
    }

    public void run() {
        //TODO: Read/write packets here
        try {
            String message = receive();
            send(message);
            socket.close();
        } catch(IOException e) {
            System.err.println(e);
        }
    }

    public void send(String mssg) throws IOException{
        //Called within run
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(mssg);
        out.flush();

    }

    public String receive() throws IOException{
        //Called within run
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Scanner sc = new Scanner(in);
        String command = sc.nextLine();
        // to-do: go through possible commands and perform correct actions
        String response = bookServer.processCommand(command);
        sc.close();
        return response;
    }

    public void processCommand(Request r) {
        switch (r.operationId) {
            case(0):
                int x = 1;
                break;
            case(1):
                bookServer.inventory.put(r.title, bookServer.inventory.get(r.title) - 1);
                int y = bookServer.loanNumber++;
                // Return message
                break;
            case(2):
                break;
            case(3):
                break;
            case(4):
                break;
            case(5):
                break;
        }
    }
}
