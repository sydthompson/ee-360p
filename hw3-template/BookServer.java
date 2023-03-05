import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.*;

public class BookServer {
    public static void main(String[] args) throws FileNotFoundException {
        int tcpPort, udpPort;
        tcpPort = 7000;
        udpPort = 8000;

        //TODO: synchronized code doesn't have to be good, just lock HashMap whenever we do something
        HashMap<String, Integer> inventory;

        if (args.length != 1) {
            System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
            System.exit(-1);
        }
        String fileName = args[0];
        File init = new File(fileName);
        inventory = parseFile(init);

        checkInventory(inventory);

        // TODO: handle request from clients
        while (true) {
            //Create socket and spawn threads based on UDP or TCP as needed from here
        }
    }

    static HashMap<String, Integer> parseFile(File file) throws FileNotFoundException {
        HashMap<String, Integer> inventory = new HashMap<>();
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
