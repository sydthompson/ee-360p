import java.io.*;
import java.net.*;

public class TcpClientHandler implements Runnable {

    Socket socket;
    PrintWriter out;
    BufferedReader in;

    public TcpClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void run() {
        //TODO: Read/write packets here
    }

    public void send() {
        //Called within run
    }

    public void receive() {
        //Called within run
    }
}
