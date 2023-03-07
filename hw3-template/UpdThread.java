import java.net.DatagramPacket;

class UdpThread implements Runnable {

    private BookServer server;
    
    public UdpThread(BookServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        //Listen first then create a thread once the message is received
        byte[] udpBuffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(udpBuffer, udpBuffer.length);
        System.out.println("Connection waiting");
        try {
            server.udpSocket.receive(packet);
            System.out.println("Connection accepted, handing off");
            /* UDP Listener */
            new Thread(new UdpClientHandler(server.udpSocket, server, packet)).start();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

}