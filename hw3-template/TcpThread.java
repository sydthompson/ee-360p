import java.net.Socket;

class TcpThread implements Runnable {

    private BookServer server;
    
    public TcpThread(BookServer server) {
        this.server = server;
    }

    @Override
    public void run() {
         // Blocks until connection established
        try {
            Socket s = server.tcpSocket.accept();
            System.out.println("Connection accepted, handing off");
            // Hand off the socket to a thread to manage the connection
            new Thread(new TcpClientHandler(s, server)).start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

}