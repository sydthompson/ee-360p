import java.net.DatagramPacket;

class UdpThread implements Runnable {

    private BookServer server;
    
    public UdpThread(BookServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        //Listen first then create a thread once the message is received
        while (true) {
            byte[] udpBuffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(udpBuffer, udpBuffer.length);
            try {
                server.udpSocket.receive(packet);
                System.out.println("UDP connection accepted, handing off");
                /* UDP Listener */
                new Thread(new UdpClientHandler(server.udpSocket, server, packet)).start();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

}