
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class BookServer {

    int tcpPort = 7000, udpPort= 8000;
    DatagramSocket udpSocket;
    ServerSocket tcpSocket;

    LinkedHashMap<String, Integer> inventory;

    public BookServer () throws IOException {
        udpSocket = new DatagramSocket(udpPort);
        tcpSocket = new ServerSocket(tcpPort);
        //TODO: synchronized code doesn't have to be good, just lock HashMap whenever we do something
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
                //TODO: How to listen to both UDP and TCP? TCP we can make a thread for every connection
                //TODO: established. But UDP seems trickier...
//                byte[] udpBuffer = new byte[2048];
//                DatagramPacket packet = new DatagramPacket(udpBuffer, udpBuffer.length);
//                server.udpSocket.receive(packet);

                Socket clientSocket = server.tcpSocket.accept();
                new Thread(new TcpClientHandler(clientSocket)).start();

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO: Maybe change below functions into part of Server class
    static LinkedHashMap<String, Integer> parseFile(File file) throws FileNotFoundException {
        LinkedHashMap<String, Integer> inventory = new LinkedHashMap<>();
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

    static void checkInventory(HashMap<String, Integer> inventory) {
        inventory.entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + ", " + entry.getValue());
        });
    }
}
