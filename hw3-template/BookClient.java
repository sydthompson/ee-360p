import java.awt.print.Book;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class BookClient {

    String hostAddress;
    int tcpPort;
    int udpPort;
    boolean isUdp;

    int clientId;

    public BookClient() throws IOException {
        this.hostAddress = "localhost";
        this.tcpPort = 7000;
        this.udpPort = 8000;
        this.isUdp = false; //TODO: Should be true
    }

    public String sendTcp(Request r) throws IOException {
        try {
            //TODO: Socket and oos should not be established here ephemerally, but sustained until set-mode is called
            Socket pa = new Socket(InetAddress.getByName(this.hostAddress), this.tcpPort);
            ObjectOutputStream oos = new ObjectOutputStream(pa.getOutputStream());
            // Write serializable object to the output stream and flush to send if connection established
            oos.writeObject(r);
            oos.flush();

            //TODO: Block for a response here
        } catch(Exception e) {
            System.err.println(e);
        } return "";
    }

    public String sendUdp(String mssg) {
        DatagramSocket dataSocket = null;
        try {
            dataSocket = new DatagramSocket();
            byte[] buffer = new byte[mssg.length()];
            buffer = mssg.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer,
                    buffer.length,
                    InetAddress.getByName(hostAddress),
                    udpPort);

            dataSocket.send(packet);

            byte[] rbuffer = new byte[2048];
            DatagramPacket rPacket = new DatagramPacket(rbuffer, rbuffer.length);
            dataSocket.receive(rPacket);
            dataSocket.close();

            return new String(rPacket.getData(), 0, rPacket.getLength());

        } catch (Exception e) {
            System.err.println(e);
        }
        return "";
    }

    public static void main(String[] args) throws IOException{

        BookClient client = new BookClient();

        if (args.length != 2) {
            System.out.println("ERROR: Provide 2 arguments: command-file, clientId");
            System.out.println("\t(1) command-file: file with commands to the server");
            System.out.println("\t(2) clientId: an integer between 1..9");
            System.exit(-1);
        }

        String commandFile = args[0];
        client.clientId = Integer.parseInt(args[1]);

        try {
            Scanner sc = new Scanner(new FileReader(commandFile));

            while (sc.hasNextLine()) {
                //Each line corresponds to one command, `r` will be piped to relevant transmission method
                Request r;

                String cmd = sc.nextLine();
                String[] tokens = cmd.split(" ");

                //
                if (tokens[0].equals("set-mode")) {                             // 0
                    r = new Request(0);
                } else if (tokens[0].equals("begin-loan")) {                    // 1
                    // Use regex pattern to avoid having to deal with quoted titles
                    Pattern p = Pattern.compile("begin-loan (.*) (\".*\")");
                    Matcher m = p.matcher(cmd);
                    m.matches();

                    String user = m.group(1);
                    String title = m.group(2);
                    System.out.println(user + " wants " + title);

                    r = new Request(1);
                    r.user = user;
                    r.title = title;
                } else if (tokens[0].equals("end-loan")) {                      // 2
                    r = new Request(2);
                } else if (tokens[0].equals("get-loans")) {                     // 3
                    r = new Request(3);
                } else if (tokens[0].equals("get-inventory")) {                 // 4
                    r = new Request(4);
                } else if (tokens[0].equals("exit")) {                          // 5
                    r = new Request(5);
                } else {
                    System.out.println("ERROR: No such command");
                    continue;
                }
                //
                if (client.isUdp) {
                } else {
                    client.sendTcp(r);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
