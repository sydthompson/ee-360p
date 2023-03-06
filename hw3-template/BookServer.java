
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class BookServer {

    int tcpPort = 7000, udpPort= 8000;
    DatagramSocket udpSocket;
    ServerSocket tcpSocket;

    ConcurrentHashMap<String, Integer> inventory;
    AtomicInteger loanNumber = new AtomicInteger(0);

    public BookServer () throws IOException {
        udpSocket = new DatagramSocket(udpPort);
        tcpSocket = new ServerSocket(tcpPort);
        inventory = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) throws IOException {
        BookServer server = new BookServer();

        if (args.length != 1) {
            System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
            System.exit(-1);
        }
        String fileName = args[0];
        File init = new File(fileName);
        server.inventory = parseFile(init);

        checkInventory(server.inventory);

        // TODO: handle request from clients
        while (true) {
            //Create socket and spawn threads based on UDP or TCP as needed from here
            try{
                //TODO: Put UDP and TCP listeners on separate threads
//                byte[] udpBuffer = new byte[2048];
//                DatagramPacket packet = new DatagramPacket(udpBuffer, udpBuffer.length);
//                server.udpSocket.receive(packet);
//
//                /* UDP Listener */
//                UdpClientHandler udpClient = new UdpClientHandler(server.udpSocket, server);
//                udpClient.run();

                /* TCP Listener */
                Socket clientSocket = server.tcpSocket.accept();

                ServerSocket listener = new ServerSocket(clientSocket.getPort());
                Socket s;
                while ( (s = listener.accept()) != null) {
                    (new TcpClientHandler(s, server)).run();
                }


            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    static ConcurrentHashMap<String, Integer> parseFile(File file) throws FileNotFoundException {

        ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();

        Scanner sc = new Scanner(file);
        Pattern p = Pattern.compile("(\".*\") (\\d+)");

        while (sc.hasNextLine()) {

            String entry = sc.nextLine();
            Matcher m = p.matcher(entry);
            m.matches();

            // Get group matches and populate inventory structure
            String title = m.group(1);
            Integer quantity = Integer.valueOf(m.group(2));
            inventory.put(title, quantity);
        }

        return inventory;
    }

    static void checkInventory(ConcurrentHashMap<String, Integer> inventory) {
        inventory.entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + ", " + entry.getValue());
        });
    }

    public String beginLoan(String userName, String bookName) {
        return "";
    }

    public String endLoan(String loanId) {
        return "";
    }

    public String getLoans(String userName) {
        return "";
    }

    public String getInventory() {
        return "";
    }

    public String exit() {
        return "";
    }
}
