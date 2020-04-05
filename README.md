# Networks-A3
By: Celestino Ballesteros

1. Run Router.go
Usage:
    router --port int --drop-rate float --max-delay duration --seed int
e.g.
router --port=3000 --drop-rate=0.2 --max-delay=10ms --seed=1


2. Run httpfs.java to have the Server Listen
httpfs is a simple file server.
    usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]
        -v   Prints debugging messages.
        -p   Specifies the port number that the server will listen and serve at.
             Default is 8080.
        -d   Specifies the directory that the server will use to read/write
             requested files. Default is the current directory when launching the
             application.
e.g.
    httpfs -v


3. Run httpc to have a Client make POST/GET requests.
httpc is a curl-like application but supports HTTP protocol only.
    Usage:
    httpc command [arguments]
    The commands are:
        get     executes a HTTP GET request and prints the response.
        post    executes a HTTP POST request and prints the response.
        help    prints this screen.
    Use "httpc help [command]" for more information about a command.
    
GET example:
	httpc get -h Content-Type:application/json -v http://localhost/fileOne.txt
            
POST example:
        httpc post -f "loremipsum.txt" -v http://localhost/fileOne.txt
