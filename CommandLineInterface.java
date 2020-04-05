
import java.util.Scanner;

public class CommandLineInterface
{

    // Command Line Application, that is using the HTTP-Client Library
    public static void main(String[ ] args)
    {
        String inputLine = null;
        Scanner reader = new Scanner(System.in);

        // Get input from user
        while(reader.hasNext() )
        {
            inputLine = reader.nextLine();
            // separate the line into splitArr
            String[] splitArr = inputLine.split(" ");


            // ***** First word is Correct - httpc *****
            if( splitArr[0].equals("httpc") && splitArr.length >= 2 )
            {
                /* ***** 1. httpc help - plus (post | get) or without a word after *****
                * 1.1 httpc help post
                * 1.2 httpc help get
                * 1.3 httpc help (without a word after)
                */
                if(splitArr.length > 2)
                {
                    // 1.1 httpc help post
                    if( splitArr[1].equals("help") && splitArr[2].equals("post") )
                    {
                        System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" +
                                "Post executes a HTTP POST request for a given URL with inline data or from file\n" +
                                "-v              Prints the detail of the response such as protocol, status, and headers.\n" +
                                "-h key:value    Associates headers to HTTP Request with the format 'key:value'.\n" +
                                "-d string       Associates an inline data to the body HTTP POST request.\n" +
                                "-f file         Associates the content of a file to the body HTTP POST request\n \n" +
                                "Either [-d] or [-f] can be used but not both.");
                    }
                    // 1.2 httpc help get
                    if( splitArr[1].equals("help") && splitArr[2].equals("get") )
                    {
                        System.out.println("usage: httpc get [-v] [-h key:value] URL\n" +
                                "Get executes a HTTP GET request for a given URL.\n" +
                                "-v              Prints the detail of the response such as protocol, status, and headers.\n" +
                                "-h key:value    Associates headers to HTTP Request with the format 'key:value'. \n");
                    }
                }
                // 1.3 httpc help (without a word after)
                if( splitArr[1].equals("help") && splitArr.length <= 2 )
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

                // With post or get
                if(splitArr.length > 2)
                {
                    String stringRequest = "";
                    for (int i=0; i<splitArr.length; i++)
                    {
                        stringRequest = stringRequest + splitArr[i] + " ";
                    }

                    /* ***** 2. httpc get - usage: httpc get [-v] [-h key:value] URL *****
                    * 2.1 httpc get 'http://httpbin.org/get?course=networking&assignment=1'
                    * 2.2 httpc get -v 'http://httpbin.org/get?course=networking&assignment=1'
                    * 2.3 httpc get -h "X-Header: Value" 'https://www.keycdn.com/'
                    * 2.4 with query
                    */
                    if(splitArr[1].equals("get"))
                    {
                        //System.out.println(stringRequest);

                        // send the string as the request
                        ClientUDP.getRequest(stringRequest);
                    }

                    /* ***** 3. httpc post - usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL *****
                    * 2.1 httpc post -v
                    * 2.2 httpc post -h
                    * 2.3 httpc post -d
                    * 2.4 httpc post -f
                    * 2.5 with query
                    * Either [-d] or [-f] can be used, BUT not both.
                    */
                    if(splitArr[1].equals("post"))
                    {
                        //System.out.println(stringRequest);

                        // send the string as the request
                        ClientUDP.postRequest(stringRequest);
                    }
                }

            } // END "httpc" good

        }

    }

}
