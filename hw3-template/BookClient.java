import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class BookClient {

    String hostAddress;
    int tcpPort;
    int udpPort;
    boolean isUdp = false;

    int clientId;

    Socket tcpPa;
    ObjectOutputStream tcpOos;
    ObjectInputStream tcpOis;

    DatagramSocket datagramSocket;

    FileWriter writer;

    public BookClient(int clientId) throws IOException {
        this.hostAddress = "localhost";
        this.tcpPort = 7000;
        this.udpPort = 8000;
        this.isUdp = true;
        this.clientId = clientId;

        this.writer = new FileWriter(new File(String.format("out_%d.txt", clientId)));

        // Starts a UDP connection
        datagramSocket = new DatagramSocket();

        // Starts a TCP connection with appropriate I/O streams
        tcpPa = new Socket(InetAddress.getByName(this.hostAddress), this.tcpPort);
        tcpOos = new ObjectOutputStream(tcpPa.getOutputStream());
        tcpOis = new ObjectInputStream(tcpPa.getInputStream());
    }

    public String sendTcp(Request r) throws IOException {
        try {
            // Write serializable object to the output stream and flush to send if connection established
            tcpOos.writeObject(r);
            tcpOos.flush();

            // Expect the server to return only string responses
            String response = (String) tcpOis.readObject();

            writer.write(response + "\n");
            writer.flush();

            return response;
        } catch(Exception e) {
            System.err.println(e);
        } return "";
    }

    public String sendUdp(Request r) throws IOException {
       try {
           byte[] buffer = getByteArray(r);

           DatagramPacket packet = new DatagramPacket(buffer,
                   buffer.length,
                   InetAddress.getByName(hostAddress),
                   udpPort);

           datagramSocket.send(packet);

           byte[] rbuffer = new byte[2048];
           DatagramPacket rPacket = new DatagramPacket(rbuffer, rbuffer.length);
           datagramSocket.receive(rPacket);

           String response = new String(rPacket.getData(), 0, rPacket.getLength());

            writer.write(response + "\n");
            writer.flush();

            return response;

       } catch (Exception e) {
           System.err.println(e);
       }
        return "";
    }

    private byte[] getByteArray(Request r) throws IOException{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream write = new ObjectOutputStream(stream);
        write.writeObject(r);
        write.flush();

        byte[] convert = stream.toByteArray();

        write.close();
        stream.close();

        return convert;
    }

    public static void main(String[] args) {

        try {

            if (args.length != 2) {
                System.out.println("ERROR: Provide 2 arguments: command-file, clientId");
                System.out.println("\t(1) command-file: file with commands to the server");
                System.out.println("\t(2) clientId: an integer between 1..9");
                System.exit(-1);
            }

            String commandFile = args[0];
            BookClient client = new BookClient(Integer.parseInt(args[1]));


            Scanner sc = new Scanner(new FileReader(commandFile));


            while (sc.hasNextLine()) {
                //Each line corresponds to one command, `r` will be piped to relevant transmission method
                Request r;
                Boolean exitStatus = false;

                String cmd = sc.nextLine().trim();
                String[] tokens = cmd.split("\\s+");

                //
                if (tokens[0].equals("set-mode")) {                             // 0
                    r = new Request(0);
                    if (tokens[1].equals("u") && client.isUdp == false) {
                        r.setUdp = true;
                        client.isUdp = true;
                    } else if (tokens[1].equals("t") && client.isUdp == true) {
                        r.setUdp = false;
                        client.isUdp = false;
                    }
                } else if (tokens[0].equals("begin-loan")) {                    // 1
                    // Use regex pattern to avoid having to deal with quoted titles
                    Pattern p = Pattern.compile("begin-loan (.*) (\".*\")");
                    Matcher m = p.matcher(cmd);
                    m.matches();
                    String user = m.group(1);
                    String title = m.group(2);

                    r = new Request(1);
                    r.user = user;
                    r.title = title;
                } else if (tokens[0].equals("end-loan")) {                   // 2
                    r = new Request(2);
                    r.loanId = Integer.parseInt(tokens[1]);
                } else if (tokens[0].equals("get-loans")) {                     // 3
                    r = new Request(3);
                    r.user = tokens[1];
                } else if (tokens[0].equals("get-inventory")) {                 // 4
                    r = new Request(4);
                } else if (tokens[0].equals("exit")) {                          // 5
                    r = new Request(5);
                    client.writer.flush();
                    exitStatus = true;
                } else {
                    System.out.println("ERROR: No such command");
                    continue;
                }
                // Send message then check for exit status
                if (client.isUdp) {
                    System.out.println(client.sendUdp(r));
                } else {
                    System.out.println(client.sendTcp(r));
                }

                if (exitStatus) {
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }
}
