import java.net.Socket;

class TcpThread implements Runnable {

    private BookServer server;
    
    public TcpThread(BookServer server) {
        this.server = server;
    }

    @Override
    public void run() {
         // Blocks until connection established
        while (true) {
            try {
                Socket s = server.tcpSocket.accept();
                System.out.println("TCP connection accepted, handing off");
                // Hand off the socket to a thread to manage the connection
                new Thread(new TcpClientHandler(s, server)).start();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

}