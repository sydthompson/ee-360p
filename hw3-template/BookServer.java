
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class BookServer {
    //TODO: UDP queue, HashMap
    int tcpPort = 7000, udpPort= 8000;
    DatagramSocket udpSocket;
    ServerSocket tcpSocket;

    ConcurrentHashMap<String, Integer> inventory;
    ConcurrentHashMap<Integer, LoanInfo> loanMap;
    AtomicInteger loanNumber = new AtomicInteger(0);

    public BookServer () throws IOException {
        udpSocket = new DatagramSocket(udpPort);
        tcpSocket = new ServerSocket(tcpPort);
        inventory = new ConcurrentHashMap<>();
        loanMap = new ConcurrentHashMap<>();
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

        while (true) {
            //Create socket and spawn threads based on UDP or TCP as needed from here
            // UDP and TCP listeners on separate threads
            (new TcpThread(server)).run();

            (new UdpThread(server)).run();
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

    String checkInventory() {
        String output = "";
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            output += String.format("%s %s\n", key, value);
        }
        return output;
    }

}
