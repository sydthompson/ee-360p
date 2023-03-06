
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

public class BookServer {

    int tcpPort = 7000, udpPort= 8000;
    DatagramSocket udpSocket;
    ServerSocket tcpSocket;

    ConcurrentHashMap<String, Integer> inventory;

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
                //TODO: How to listen to both UDP and TCP? TCP we can make a thread for every connection
                //TODO: established. But UDP seems trickier...
//                byte[] udpBuffer = new byte[2048];
//                DatagramPacket packet = new DatagramPacket(udpBuffer, udpBuffer.length);
//                server.udpSocket.receive(packet);

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

    //TODO: Maybe change below functions into part of Server class
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

    public String processCommand(String mssg) {
        String response = "";
        String[] split = mssg.split( "\\s+");
        switch(split[0]) {
           case "begin-loan": response = beginLoan(split[1], split[2]);
           case "end-loan": response = endLoan(split[1]);
           case "get-loans": response = getLoans(split[1]);
           case "get-inventory": response = getInventory();
           case "exit": response = exit();
        }
        return response;
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
