import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class BookClient {
    public static void main(String[] args) {

        String hostAddress;
        int tcpPort;
        int udpPort;
        int clientId;
        boolean isUpd = true;

        if (args.length != 2) {
            System.out.println("ERROR: Provide 2 arguments: command-file, clientId");
            System.out.println("\t(1) command-file: file with commands to the server");
            System.out.println("\t(2) clientId: an integer between 1..9");
            System.exit(-1);
        }

        String commandFile = args[0];
        clientId = Integer.parseInt(args[1]);
        hostAddress = "localhost";
        tcpPort = 7000;// hardcoded -- must match the server's tcp port
        udpPort = 8000;// hardcoded -- must match the server's udp port

        try {
            Scanner sc = new Scanner(new FileReader(commandFile));

            while (sc.hasNextLine()) {
                String cmd = sc.nextLine();
                String[] tokens = cmd.split(" ");

                if (tokens[0].equals("set-mode")) {
                    // TODO: set the mode of communication for sending commands to the server
                } else if (tokens[0].equals("begin-loan")) {
                    // Use regex pattern to avoid
                    Pattern p = Pattern.compile("begin-loan (.*) (\".*\")");
                    Matcher m = p.matcher(cmd);
                    m.matches();

                    String user = m.group(1);
                    String title = m.group(2);
                    System.out.println(user + " wants " + title);
                } else if (tokens[0].equals("end-loan")) {
                    // TODO: send appropriate command to the server and display the
                    // appropriate responses form the server
                } else if (tokens[0].equals("get-loans")) {
                    // TODO: send appropriate command to the server and display the
                    // appropriate responses form the server
                } else if (tokens[0].equals("get-inventory")) {

                } else if (tokens[0].equals("exit")) {
                    // TODO: send appropriate command to the server
                } else {
                    System.out.println("ERROR: No such command");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String sendUpd(String mssg, int clientId, int updPort, String hostAddress) {
        DatagramSocket dataSocket = null;
        try {
            dataSocket = new DatagramSocket();
            byte[] buffer = new byte[mssg.length()];
            buffer = mssg.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, 
                                                       buffer.length, 
                                                       InetAddress.getByName(hostAddress), 
                                                       updPort);

            dataSocket.send(packet);

            byte[] rbuffer = new byte[2048];
            DatagramPacket rPacket = new DatagramPacket(rbuffer, rbuffer.length);
            dataSocket.receive(rPacket);
            dataSocket.close();

            return new String(rPacket.getData(), 0, rPacket.getLength());

        } catch(Exception e) {
            System.err.println(e);
        } return "";
    }

    public static String sendTcp(String mssg, int clientId, int tcpPort, String hostAddress) {
        try {
            Socket pa = new Socket(InetAddress.getByName(hostAddress), tcpPort);
            PrintWriter pout = new PrintWriter(pa.getOutputStream());
            pout.println(mssg);
            pout.flush();

            Scanner sc = new Scanner(pa.getInputStream());
            String response = "";
            while(sc.hasNextLine() && !sc.nextLine().equals("end")) {
                response += sc.nextLine();
            }

            sc.close();
            pa.close();
            return response;

        } catch(Exception e) {
            System.err.println(e);
        } return "";
    }
}
