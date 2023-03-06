import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClientHandler implements Runnable {

    Socket socket;


    public TcpClientHandler(Socket socket)  {
        this.socket = socket;
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

        sc.close();
        return " ";
    }
}
