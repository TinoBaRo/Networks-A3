
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ClientUDP
{

    // GET
    public static void getRequest(String stringRequest)
    {
        boolean SYN = false;
        boolean GET = false;
        boolean FIN = false;

        int sequenceNumb = 100;

        // UNRELIABLE DATA TRANSFER (UDP)
        DatagramChannel channel;
        InetAddress serverAddress;
        SocketAddress routerAddr;
        Set<SelectionKey> keys;


        // usual ...
        String[] splitArr = stringRequest.split(" ");

        String urlPath = null;
        String version = "HTTP/1.0";
        Vector headers = new Vector();
        // String entityBody = null; // no entity body in get method (there is one for the post)

        // GET THE URL
        // starts looking after httpc
        for (int i = 1; i < splitArr.length; i++) {
            if (splitArr[i].contains("http")) {
                urlPath = splitArr[i];
            }
        }
        // get the headers
        for (int i = 1; i < splitArr.length; i++) {
            if (splitArr[i].equals("-h")) {
                headers.add(splitArr[i+1] + "\r\n"); // the element after -h is "Header:Value"
            }
        }
        String headerLine = "";
        if(headers.size() > 0) {
            for (int i = 0; i < headers.size(); i++) {
                headerLine += headers.elementAt(i);
            }
        }


        // ----- Establish connection -----
        try
        {
            // open channel
            channel = DatagramChannel.open();

            // usual ...
            URL url = new URL(urlPath);
            InetAddress address = InetAddress.getByName(url.getHost());

            String temp = address.toString();
            //System.out.println("temp: " + temp);

            String[] splitAddress = temp.split("/");
            String ip = splitAddress[splitAddress.length - 1];

            String responseString = "";

            if (ip != null)
            {
                // msg
                String msg = "GET " + url.getPath() + " HTTP/1.0\r\n" + headerLine + "\r\n\r\n"; // MAY have an extra one since headerLine finishes with "\r\n"
                // server
                serverAddress = InetAddress.getByName("localhost");
                int serverPort = 8080;
                // router
                routerAddr = new InetSocketAddress("localhost", 3000);


                /* ---------- STARTS THREE WAY HANDSHAKE ----------
                 * There could be DIFFERENT TYPE of packet:
                 * 0:Data, 1:ACK, 2:NAK, 3:SYN, 4:SYN-ACK, 5:FIN, 6:FIN-ACK
                 */

                // 1.0 ---------- SND SYN ----------
                while(SYN == false)
                {
                    try
                    {
                        sequenceNumb++;

                        Packet p1 = new Packet.Builder()
                                .setType(3) // SYN
                                .setSequenceNumber(sequenceNumb)
                                .setPortNumber(serverPort)
                                .setPeerAddress(serverAddress)
                                .setPayload("".getBytes())
                                .create();
                        // Send Packet with Message
                        channel.send(p1.toBuffer(), routerAddr);
                        //logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
                        System.out.println("Sending to Router Packet with SYN");


                        // 2. ---------- RECEIVE SYN-ACK ----------
                        // IF NOT RECEIVED, RE-SEND SYN
                        channel.configureBlocking(false);
                        Selector selector = Selector.open();
                        channel.register(selector, OP_READ);

                        System.out.println("Waiting for the response ... \n");
                        selector.select(5000);

                        keys = selector.selectedKeys();
                        if (keys.isEmpty())
                        {
                            System.out.println("No response after timeout \n SYN will be re-sent \n");

                            // SYN still false
                        }
                        else
                        {
                            SYN = true; // SYN got a response back

                            // Response from Server is supposed to be SYN-ACK
                            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                            SocketAddress router = channel.receive(buf);
                            buf.flip();

                            Packet resp = Packet.fromBuffer(buf);

                            int respType = resp.getType();
                            long respSeqNumb = resp.getSequenceNumber();

                            if(respType == 4 && respSeqNumb == sequenceNumb ) // received SYN-ACK
                            {
                                System.out.println("Received Server's Packet with SYN-ACK \n");
                            }
                            keys.clear();
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                } // FINISH 3-way-handshake


                // 3. ------------ SEND GET ------------
                // there could be DIFFERENT TYPES of packet: i.e. Data, ACK, NAK, SYN, SYN-ACK
                while(GET==false)
                {
                    sequenceNumb++;

                    Packet p = new Packet.Builder()
                            .setType(0)
                            .setSequenceNumber(sequenceNumb)
                            .setPortNumber(serverPort)
                            .setPeerAddress(serverAddress)
                            .setPayload(msg.getBytes())
                            .create();

                    // Send Packet with Message
                    channel.send(p.toBuffer(), routerAddr);

                    //logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
                    System.out.println("Sending to Router Packet with msg: \n" + msg);

                    // Receive Packet with Message
                    // Try to receive a packet within timeout.
                    channel.configureBlocking(false);
                    Selector selector = Selector.open();
                    channel.register(selector, OP_READ);

                    //logger.info("Waiting for the response");
                    System.out.println("Waiting for the response ... \n");
                    selector.select(5000);

                    keys = selector.selectedKeys();
                    if (keys.isEmpty())
                    {
                        //logger.error("No response after timeout");
                        System.out.println("No response after timeout \n Data-Packet will be re-sent\n");

                        // GET still false
                    }
                    else
                    {
                        GET = true;

                        System.out.println("Received Server's Packet with DATA - Data will be shown after closing connection (according to httpc requirements) \n");

                        // We just want a single response.
                        ByteBuffer buf_2 = ByteBuffer.allocate(Packet.MAX_LEN);
                        SocketAddress router = channel.receive(buf_2);
                        buf_2.flip();

                        Packet resp_2 = Packet.fromBuffer(buf_2);

                        String payload = new String(resp_2.getPayload(), StandardCharsets.UTF_8);

                        responseString = payload;
                        //System.out.println(responseString);

                        keys.clear();
                    }

                } // GET


                // 4.0 ------ FINALLY, close connection ------
                // FIN ->
                //     <- FIN-ACK
                while(FIN == false)
                {
                    sequenceNumb++;

                    Packet p = new Packet.Builder()
                            .setType(5) // FIN
                            .setSequenceNumber(sequenceNumb)
                            .setPortNumber(serverPort)
                            .setPeerAddress(serverAddress)
                            .setPayload("".getBytes())
                            .create();

                    // Send Packet with Message
                    channel.send(p.toBuffer(), routerAddr);

                    //logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
                    System.out.println("Sending to Router Packet with FIN (to close connection) \n");

                    // Receive Packet with Message
                    // Try to receive a packet within timeout.
                    channel.configureBlocking(false);
                    Selector selector = Selector.open();
                    channel.register(selector, OP_READ);

                    //logger.info("Waiting for the response");
                    System.out.println("Waiting for the response ... \n");
                    selector.select(5000);

                    keys = selector.selectedKeys();
                    if (keys.isEmpty())
                    {
                        //logger.error("No response after timeout");
                        System.out.println("No response after timeout \n FIN will be re-sent \n");

                        // FIN still false
                    }
                    else
                    {
                        FIN = true;

                        // We just want a single response.
                        ByteBuffer buf_3 = ByteBuffer.allocate(Packet.MAX_LEN);
                        SocketAddress router = channel.receive(buf_3);
                        buf_3.flip();

                        Packet resp = Packet.fromBuffer(buf_3);

                        int respType = resp.getType();
                        long respSeqNumb = resp.getSequenceNumber();

                        if(respType == 6 && respSeqNumb == sequenceNumb ) // received FIN-ACK
                        {
                            System.out.println("Received Server's Packet with FIN-ACK \n");
                            System.out.println("* * * THE CONNECTION IS CLOSED NOW * * * \n");
                        }

                        keys.clear();
                    }

                }

            }

            // --------- RESPONSE CONSOLE ---------
            //System.out.print("RESPONSE: \n" + responseString + "\n");

            // SPLIT the response, to get the Entity Body of the response completely separate
            String[] splitResponse = responseString.split("\r\n\r\n");

            // httpc get http://localhost/fileOne.txt
            // httpc get http://httpbin.org/get?course=networking&assignment=1
            if (!stringRequest.contains("-v"))
            {
                //since there is no -v, ONLY display the Entity Body of the response
                if (splitResponse.length > 1)
                {
                    System.out.println(splitResponse[1]); // only show entity body
                } else
                    System.out.println("No entity Body");
            }
            // httpc get -h Content-Type:application/json -v http://localhost/fileOne.txt
            // httpc get -v http://localhost/fileOne.txt
            // httpc get -h Content-Type:application/json -v http://httpbin.org/get?course=networking&assignment=1
            else if (stringRequest.contains("-v"))
            {
                if(stringRequest.contains("-v") == true )
                {
                    System.out.println(responseString); // entire response
                }
            }
            else
            {
                System.out.println("httpc is a curl-like application but supports HTTP protocol only.\n" +
                        "Usage:\n" +
                        "    httpc command [arguments]\n" +
                        "The commands are:\n" +
                        "    get     executes a HTTP GET request and prints the response.\n" +
                        "    post    executes a HTTP POST request and prints the response.\n" +
                        "    help    prints this screen.\n" +
                        "Use \"httpc help [command]\" for more information about a command.\n");
            }

        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (ClosedChannelException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }


    }// END METHOD-GET



    // POST
    public static void postRequest(String stringRequest)
    {
        boolean SYN = false;
        boolean POST = false;
        boolean FIN = false;

        int sequenceNumb = 100;

        // UNRELIABLE DATA TRANSFER (UDP)
        DatagramChannel channel;
        InetAddress serverAddress;
        SocketAddress routerAddr;
        Set<SelectionKey> keys;


        // usual ...
        String[] splitArr = stringRequest.split(" ");

        String urlPath = null;
        String version = "HTTP/1.0";
        Vector headers = new Vector();
        String entityBody = ""; // we also care about the length of the entity-body
        String contentLengthHeader = "";

        // header-line
        String headerLine = "";

        // GET THE URL
        for (int i = 1; i < splitArr.length; i++)
        {
            if (splitArr[i].contains("http"))
            {
                urlPath = splitArr[i];
            }
        }
        // get headers
        if(stringRequest.contains("-h"))
        {
            for (int i = 1; i < splitArr.length; i++)
            {
                if (splitArr[i].equals("-h"))
                {
                    headers.add(splitArr[i + 1] + "\r\n"); // the element after -h is "Header:Value"
                }
            }
            if (headers.size() > 0)
            {
                for (int i = 0; i < headers.size(); i++)
                {
                    headerLine += headers.elementAt(i);
                }
            }
        }
        // get inline data
        if(stringRequest.contains("-d"))
        {
            // "Entity-Body", it's the inline data
            entityBody = stringRequest.substring(stringRequest.indexOf("{"), stringRequest.indexOf("}") + 1);

            contentLengthHeader = "Content-Length:" + Integer.toString(entityBody.length()) + "\r\n";
            // Add it to the header-line
            headerLine += contentLengthHeader;
        }
        // get data in file
        if(stringRequest.contains("-f"))
        {
            /* "Entity-Body", it's the String in the given file
             * The file is in between "loremipsum.txt"
             */
            String fileName = stringRequest.substring( stringRequest.indexOf("\"")+1 , stringRequest.lastIndexOf("\""));

            try
            {
                String data = "";
                data = new String(Files.readAllBytes(Paths.get("/Users/Celestino/Desktop/"+fileName)));
                //System.out.println(data);

                // Entity-body is the data inside the given file (file has desktop path)
                entityBody = data;
            }
            catch (IOException e)
            {
                System.out.println(e);
            }

            contentLengthHeader = "Content-Length:" + Integer.toString( entityBody.length() ) + "\r\n";
            // Add it to the header-line
            headerLine += contentLengthHeader;
        }


        // ----- Establish connection -----
        try
        {
            // open channel
            channel = DatagramChannel.open();


            // usual ...
            URL url = new URL(urlPath);
            InetAddress address = InetAddress.getByName(url.getHost());

            String temp = address.toString();
            //System.out.println("temp: " + temp);

            String[] splitAddress = temp.split("/");
            String ip = splitAddress[splitAddress.length - 1];

            String responseString = "";


            if (ip != null)
            {
                // msg
                // NOTE, last headerLine has another "\r\n"
                String msg = "POST " + url.getPath() + " HTTP/1.0\r\n" + headerLine + "\r\n" + entityBody;
                // server
                serverAddress = InetAddress.getByName("localhost");
                int serverPort = 8080;
                // router
                routerAddr = new InetSocketAddress("localhost", 3000);


                /* ---------- STARTS THREE WAY HANDSHAKE ----------
                 * There could be DIFFERENT TYPE of packet:
                 * 0:Data, 1:ACK, 2:NAK, 3:SYN, 4:SYN-ACK, 5:FIN, 6:FIN-ACK
                 */

                // 1.0 ---------- SND SYN ----------
                while(SYN == false)
                {
                    try
                    {
                        sequenceNumb++;

                        Packet p1 = new Packet.Builder()
                                .setType(3) // SYN
                                .setSequenceNumber(sequenceNumb)
                                .setPortNumber(serverPort)
                                .setPeerAddress(serverAddress)
                                .setPayload("".getBytes())
                                .create();
                        // Send Packet with Message
                        channel.send(p1.toBuffer(), routerAddr);
                        //logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
                        System.out.println("Sending to Router Packet with SYN");


                        // 2. ---------- RECEIVE SYN-ACK ----------
                        // IF NOT RECEIVED, RE-SEND SYN
                        channel.configureBlocking(false);
                        Selector selector = Selector.open();
                        channel.register(selector, OP_READ);

                        System.out.println("Waiting for the response ... \n");
                        selector.select(5000);

                        keys = selector.selectedKeys();
                        if (keys.isEmpty())
                        {
                            System.out.println("No response after timeout \n SYN will be re-sent \n");

                            // SYN still false
                        }
                        else
                        {
                            SYN = true; // SYN got a response back

                            // Response from Server is supposed to be SYN-ACK
                            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                            SocketAddress router = channel.receive(buf);
                            buf.flip();

                            Packet resp = Packet.fromBuffer(buf);

                            int respType = resp.getType();
                            long respSeqNumb = resp.getSequenceNumber();

                            if(respType == 4 && respSeqNumb == sequenceNumb ) // received SYN-ACK
                            {
                                System.out.println("Received Server's Packet with SYN-ACK \n");
                            }
                            keys.clear();
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                } // FINISH 3-way-handshake


                // 3. ------------ SEND POST ------------
                while(POST==false)
                {
                    sequenceNumb++;

                    Packet p = new Packet.Builder()
                            .setType(0) // POST-DATA
                            .setSequenceNumber(sequenceNumb)
                            .setPortNumber(serverPort)
                            .setPeerAddress(serverAddress)
                            .setPayload(msg.getBytes())
                            .create();

                    // Send Packet with Message
                    channel.send(p.toBuffer(), routerAddr);

                    //logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
                    System.out.println("Sending to Router Packet with msg: \n" + msg);

                    // Receive Packet with Message
                    // Try to receive a packet within timeout.
                    channel.configureBlocking(false);
                    Selector selector = Selector.open();
                    channel.register(selector, OP_READ);

                    //logger.info("Waiting for the response");
                    System.out.println("Waiting for the response ... \n");
                    selector.select(5000);

                    keys = selector.selectedKeys();
                    if (keys.isEmpty())
                    {
                        //logger.error("No response after timeout");
                        System.out.println("No response after timeout \n Data-Packet will be re-sent\n");

                        // GET still false
                    }
                    else
                    {
                        POST = true;

                        System.out.println("Received Server's Packet with DATA - Data will be shown after closing connection (according to httpc requirements) \n");

                        // We just want a single response.
                        ByteBuffer buf_2 = ByteBuffer.allocate(Packet.MAX_LEN);
                        SocketAddress router = channel.receive(buf_2);
                        buf_2.flip();

                        Packet resp_2 = Packet.fromBuffer(buf_2);

                        String payload = new String(resp_2.getPayload(), StandardCharsets.UTF_8);
                        responseString = payload;
                        //System.out.println(responseString);

                        keys.clear();
                    }

                } // POST


                // 4.0 ------ FINALLY, close connection ------
                // FIN ->
                //     <- FIN-ACK
                while(FIN == false)
                {
                    sequenceNumb++;

                    Packet p = new Packet.Builder()
                            .setType(5) // FIN
                            .setSequenceNumber(sequenceNumb)
                            .setPortNumber(serverPort)
                            .setPeerAddress(serverAddress)
                            .setPayload("".getBytes())
                            .create();

                    // Send Packet with Message
                    channel.send(p.toBuffer(), routerAddr);

                    //logger.info("Sending \"{}\" to router at {}", msg, routerAddr);
                    System.out.println("Sending to Router Packet with FIN (to close connection) \n");

                    // Receive Packet with Message
                    // Try to receive a packet within timeout.
                    channel.configureBlocking(false);
                    Selector selector = Selector.open();
                    channel.register(selector, OP_READ);

                    //logger.info("Waiting for the response");
                    System.out.println("Waiting for the response ... \n");
                    selector.select(5000);

                    keys = selector.selectedKeys();
                    if (keys.isEmpty())
                    {
                        //logger.error("No response after timeout");
                        System.out.println("No response after timeout \n FIN will be re-sent \n");

                        // FIN still false
                    }
                    else
                    {
                        FIN = true;

                        // We just want a single response.
                        ByteBuffer buf_3 = ByteBuffer.allocate(Packet.MAX_LEN);
                        SocketAddress router = channel.receive(buf_3);
                        buf_3.flip();

                        Packet resp = Packet.fromBuffer(buf_3);

                        int respType = resp.getType();
                        long respSeqNumb = resp.getSequenceNumber();

                        if(respType == 6 && respSeqNumb == sequenceNumb ) // received FIN-ACK
                        {
                            System.out.println("Received Server's Packet with FIN-ACK \n");
                            System.out.println("* * * THE CONNECTION IS CLOSED NOW * * * \n");
                        }

                        keys.clear();
                    }

                } // FIN

            }

            // --------- RESPONSE CONSOLE ---------
            //System.out.print("RESPONSE: \n" + responseString + "\n");

            String[] splitResponse = responseString.split("\r\n\r\n");

            // httpc post -d {Assignment: 1} -v -h Content-Type:application/json http://localhost/fileOne.txt
            // httpc post -d {Assignment: 1} -v -h Content-Type:application/json http://httpbin.org/post
            // ONLY -d
            if( stringRequest.contains("-d") && !stringRequest.contains("-f") )
            {
                if(stringRequest.contains("-v") == true )
                {
                    System.out.println(responseString); // entire response
                }
                else if (splitResponse.length > 1) // ENSURE THERE IS ENTITY BODY
                {
                    System.out.println(splitResponse[1]);
                } else
                    System.out.println("No entity Body");

            }

            // ONLY -f
            // httpc post -f "loremipsum.txt" -v http://localhost/fileOne.txt
            // httpc post -f "loremipsum.txt" -v http://httpbin.org/fileOne.txt
            else if( stringRequest.contains("-f") && !stringRequest.contains("-d") )
            {
                if(stringRequest.contains("-v") == true )
                {
                    System.out.println(responseString); // entire response
                }
                else if (splitResponse.length > 1) // ENSURE THERE IS ENTITY BODY
                {
                    System.out.println(splitResponse[1]);
                } else
                    System.out.println("No entity Body");

            }

            // not correct usage
            else
            {
                System.out.println("httpc is a curl-like application but supports HTTP protocol only.\n" +
                        "Usage:\n" +
                        "    httpc command [arguments]\n" +
                        "The commands are:\n" +
                        "    get     executes a HTTP GET request and prints the response.\n" +
                        "    post    executes a HTTP POST request and prints the response.\n" +
                        "    help    prints this screen.\n" +
                        "Use \"httpc help [command]\" for more information about a command.\n");
            }


        }
        catch (MalformedURLException e)
        {
            e.getMessage();
        }
        catch (UnknownHostException e2)
        {
            e2.getMessage();
        }
        catch (IOException e)
        {
            e.getMessage();
        }


    } // END METHOD-POST


} // END ClientUDP Class