
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class ServerUDP
{

    public static void listenAndServe(int port, String directory, boolean verbose) // listening at port 8080 (default port #)
    {

        // There is no "serverSocket.accept()", it is connectionless
        try (DatagramChannel channel = DatagramChannel.open())
        {
            channel.bind(new InetSocketAddress(port));
            System.out.println("File Server is listening at: " + channel.getLocalAddress());

            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            // LOOP
            for ( ; ; )
            {
                buf.clear();
                SocketAddress router = channel.receive(buf); // receive

                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();


                // -------------------- Start Processing --------------------
                // All variables from Request
                String requestLine = "";
                String requestMethod = "";
                String requestPath = "";
                String requestVersion = "HTTP/1.0";
                String requestHeaders = "";
                String requestEntityBody = "";

                // All variables for Response
                String entireResponse = "";
                String responseVersion = "HTTP/1.0";
                String responseStatusCode = "";
                String responsePhrase = "";
                String responseHeaders = "";
                String responseEntityBody = "";
                // response sent to Client
                String responseMsg = "";


                // We are going to parse the "payload"
                String requestString = new String(packet.getPayload(), UTF_8);

                String [] splitRequest = requestString.split("\r\n");

                String [] splitEntityBody = requestString.split("\r\n\r\n");

                String [] excludeEntityBody = splitEntityBody[0].split("\r\n");


                /* ---------- THREE WAY HANDSHAKE ----------
                 * There could be DIFFERENT TYPE of packet:
                 * 0:Data, 1:ACK, 2:NAK, 3:SYN, 4:SYN-ACK, 5:FIN, 6:FIN-ACK
                 */
                // CHECK packetType
                //1.0
                if(packet.getType() == 3) // CLIENT SENT SYN (3-way-handshake)
                {
                    Packet resp = packet.toBuilder()
                            .setType(4) // SYN-ACK
                            .setSequenceNumber(packet.getSequenceNumber())
                            .create();
                    // SEND
                    channel.send(resp.toBuffer(), router);
                }

                //2.0
                if(packet.getType() == 0) // could be GET or POST
                {
                    // 1. Parse Request (First Line)
                    requestLine = splitRequest[0];
                    String[] splitLine = requestLine.split(" ");

                    requestMethod = splitLine[0];
                    requestPath = splitLine[1];

                    // 2. Get the Headers and the Entity Body
                    if (requestMethod.equals("POST") || requestMethod.contains("POST")) // POST has Entity Body
                    {
                        requestEntityBody = splitEntityBody[1];
                    }

                    // At least one header
                    if (excludeEntityBody.length > 1) {
                        for (int i = 1; i < excludeEntityBody.length; i++) {
                            requestHeaders = requestHeaders + excludeEntityBody[i] + "\n"; // added a new line between headers
                        }
                    }


                    if (verbose == true)
                    {
                        System.out.println("New Client Request: \n" + requestLine + "\n" + requestHeaders + "\n" + requestEntityBody);
                    }

                    // -------------------- GET --------------------
                    if (requestMethod.equals("GET")) {
                        // 1.0
                        // If the requestPath is "/"
                        if (requestPath.equals("/")) {
                            responseStatusCode = "200";
                            responsePhrase = "OK";
                            responseHeaders = requestHeaders;

                            File folder = new File(directory);
                            File[] listOfFiles = folder.listFiles();

                            for (File currentFile : listOfFiles) {
                                if (currentFile.isFile()) {
                                    responseEntityBody = responseEntityBody + currentFile.getName().toString() + "\n";
                                }
                            }
                        } else {
                            // 2.0
                            // Check if the file exists (coming from the "requestPath"), and if it does get the data from there
                            File wantedFile = new File(directory + requestPath);
                            if (wantedFile.exists()) {
                                try {
                                    BufferedReader fr = new BufferedReader(new FileReader(wantedFile));

                                    StringBuilder sb = new StringBuilder();
                                    String currentLine = fr.readLine();

                                    while (currentLine != null) {
                                        sb.append(currentLine);
                                        sb.append(System.lineSeparator());
                                        currentLine = fr.readLine();
                                    }
                                    responseStatusCode = "200";
                                    responsePhrase = "OK";
                                    responseEntityBody = sb.toString();
                                    responseHeaders = requestHeaders + "Content-Length:" + responseEntityBody.length(); // Plus Content-Length header

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            // If it doesnt then 404 : Not Found
                            else {
                                responseStatusCode = "404";
                                responsePhrase = "Not Found";
                            }
                        }

                        // Write Response for GET
                        responseMsg = responseVersion + " " + responseStatusCode + " " + responsePhrase + "\r\n" + responseHeaders + "\r\n" + "\r\n" + responseEntityBody;

                        // Send the response to the router not the client.
                        // The peer address of the packet is the address of the client already.
                        Packet resp = packet.toBuilder()
                                .setType(0)
                                .setPayload(responseMsg.getBytes())
                                .create();
                        // SEND
                        channel.send(resp.toBuffer(), router);

                    }// GET


                    // -------------------- POST --------------------
                    if (requestMethod.equals("POST"))
                    {
                        // Cases:
                        // 1.0 File exists, write data to file
                        // 2.0 File doesnt exist, create file and write data to file

                        if (new File(directory + requestPath).isFile())
                        {
                            File wantedFile = new File(directory + requestPath);
                            BufferedWriter fw = new BufferedWriter(new PrintWriter(new FileWriter(wantedFile, false)));

                            responseStatusCode = "200";
                            responsePhrase = "OK";
                            responseHeaders = requestHeaders;

                            fw.write(requestEntityBody); // write to the file
                            fw.flush();
                            fw.close();
                        }
                        else
                        {
                            File wantedFile = new File(directory + requestPath);
                            BufferedWriter fw = new BufferedWriter(new PrintWriter(new FileWriter(wantedFile, false)));

                            responseStatusCode = "201";
                            responsePhrase = "Created";
                            responseHeaders = requestHeaders;

                            wantedFile.mkdirs();

                            fw.write(requestEntityBody); // write to the file
                            fw.flush();
                            fw.close();
                        }

                        // Write Response for POST
                        responseMsg = responseVersion + " " + responseStatusCode + " " + responsePhrase + "\r\n" + responseHeaders + "\r\n" + "\r\n" + responseEntityBody;

                        // Send the response to the router not the client.
                        // The peer address of the packet is the address of the client already.
                        Packet resp = packet.toBuilder()
                                .setType(0)
                                .setPayload(responseMsg.getBytes())
                                .create();
                        // SEND
                        channel.send(resp.toBuffer(), router);

                    }// POST

                } // 0:DATA


                //3.0 (client wants to close connection)
                if(packet.getType() == 5) // FIN
                {
                    Packet resp = packet.toBuilder()
                            .setType(6) // FIN-ACK
                            .setSequenceNumber(packet.getSequenceNumber())
                            .create();
                    // SEND
                    channel.send(resp.toBuffer(), router);
                }

            } // Server Waiting in Loop for New Requests

        }
        catch(IOException io)
        {
            io.getMessage();
        }

    }


} // END ServerUDP Class
