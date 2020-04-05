
import java.util.Scanner;

public class httpfs
{
    public static void main(String[] args)
    {
        /*
        httpfs is a simple file server.
        usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]
           -v   Prints debugging messages.
           -p   Specifies the port number that the server will listen and serve at.
                Default is 8080.
           -d   Specifies the directory that the server will use to read/write
                requested files. Default is the current directory when launching the
                application.
        */

        String inputLine = null;
        Scanner reader = new Scanner(System.in);
        boolean verbose = false;
        int portNum = 8080; // default port number
        String directory = "/Users/Celestino/Desktop/FileServer_DefaultDirectory"; // default directory


        // Get Input - Main Loop
        while(reader.hasNext())
        {
            inputLine = reader.nextLine();


            String[] splitArr = inputLine.split(" ");
            // separate the line into splitArr

                // Call to initiate a Socket, from the Http_Server
                if (splitArr[0].equals("httpfs"))
                {
                    // If no parameters are given
                    if( !inputLine.contains("-d") &&  !inputLine.contains("-p") && !inputLine.contains("-v") && !inputLine.contains("help"))
                    {
                        // don't split the Array. Skip
                        System.out.println("inputLine: " + inputLine);
                        System.out.println("Port: " + portNum);
                        System.out.println("Directory: " + directory);
                        System.out.println("");

                        // 1.0 Initialize Server Socket
                        ServerUDP.listenAndServe(portNum, directory, verbose);
                    }
                    // help: display usage of httpfs
                    if (splitArr[1].equals("help"))
                    {
                        System.out.println("httpfs is a simple file server. \n" + "usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR] \n");
                        System.out.println("\t -v Prints debugging messages.");
                        System.out.println("\t -p Specifies the port number that the server will listen and serve at. \n" +
                                "\t    Default is 8080.");
                        System.out.println("\t -d Specifies the directory that the server will use to read/write\n" +
                                "\t    requested files. Default is the current directory when launching the\n" +
                                "\t    application.");
                    }
                    else
                    {
                        // -v: verbose option (enough information to be able to know what is happening in server)
                        if (inputLine.contains("-v")) // I don't care about the order, just that it is there.
                        {
                            verbose = true;
                        }

                        // -p: specify port number (otherwise the default is set to: 8080)
                        if (inputLine.contains("-p")) {
                            for (int i = 0; i < splitArr.length; i++) {
                                if (splitArr[i].equals("-p")) {
                                    // also change the port # for the client.
                                    // verify that the port number is valid (SOCKET WILL THROW EXCEPTION)
                                    portNum = Integer.parseInt(splitArr[i + 1]);
                                }
                            }
                        }

                        // -d: specify the directory that the server will use to read and write (otherwise use a default one)
                        // default directory is the one of the application
                        if (inputLine.contains("-d")) {
                            for (int i = 0; i < splitArr.length; i++) {
                                if (splitArr[i].equals("-d")) {
                                    directory = splitArr[i + 1];

                                    // if the user enters a directory that is not belonging to the file Server, then we will display a message
                                    if (!directory.equals("/Users/Celestino/Desktop/FileServer_Directory") && !directory.equals("/Users/Celestino/Desktop/FileServer_DefaultDirectory")) {
                                        // user entered non-valid directory
                                        System.out.println("User entered a non-valid directory, or a directory out of reach for the user.");
                                        System.out.println("The default directory will be used.");
                                    }
                                }
                            }
                        }

                        System.out.println("inputLine: " + inputLine);
                        System.out.println("Port: " + portNum);
                        System.out.println("Directory: " + directory);
                        System.out.println("");

                        // 1.0 Initialize Server Socket
                        ServerUDP.listenAndServe(portNum, directory, verbose);
                    }

                }// httpfs


        }
        // END WHILE

    }
    // END main()

}
