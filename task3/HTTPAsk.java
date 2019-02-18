import java.lang.reflect.Array;
import java.net.*;
import java.io.*;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HTTPAsk {
    private ServerSocket serverSocket;
    public static void main(String[] args) throws IOException {
        HTTPAsk server = new HTTPAsk(Integer.parseInt(args[0]));
        server.startServer();
    }

    private HTTPAsk(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    private void startServer() throws IOException {
        System.out.println("Server started!");
        while (true) {
            Socket socket = this.serverSocket.accept();
            System.out.println("New request found, dispatching worker...");
            Worker connection = new Worker(socket, "\r\n");

            Thread request = new Thread(connection);
            request.start();
        }
    }

    class Worker implements Runnable {
        private Socket socket;
        private String lineEnding;
        private String requestAction;
        private String requestHost;
        private Integer requestPort;
        private String requestParams;


        Worker(Socket socket, String lineEnding) {
            this.socket = socket;
            this.lineEnding = lineEnding;
            this.requestAction = null;
            this.requestHost = null;
            this.requestPort = null;
            this.requestParams = null;
        }

        @Override
        public void run() {
            System.out.println("Worker started working with request on socket " + this.socket.getPort());
            try {
                InputStream input = this.socket.getInputStream();
                DataOutputStream output = new DataOutputStream(this.socket.getOutputStream());
                handleRequest(input, output);
                input.close();
                output.close();
            } catch (IOException e){
                System.out.println("Worker encountered error: ");
                e.printStackTrace();
            }

        }

        private void handleRequest(InputStream in, DataOutputStream out) throws IOException {
            BufferedReader bf = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String httpRequest = bf.readLine();
            if(!httpRequest.startsWith("GET") || httpRequest.split(" ").length < 3){
                out.writeBytes(createResponse("400 Bad Request", "This server only supports GET requests with a path!"));
            } else {
                extractRequestParams(httpRequest);
                doAction(out);
            }
        }

        private void extractRequestParams(String getRequest) throws MalformedURLException {
            URL url = new URL("http://unused.com" + getRequest.split(" ")[1]);
            this.requestAction = url.getPath().substring(1);
            if(url.getQuery() != null && url.getQuery().length() > 0) {
                ArrayList<String> queryParams = new ArrayList<>(Arrays.asList(url.getQuery().split("&")));
                HashMap<String, String> params = new HashMap<>();
                for (String queryParam : queryParams) {
                    params.put(queryParam.split("=")[0], queryParam.split("=")[1]);
                }
                this.requestHost = params.get("hostname");
                this.requestPort = (params.get("port") != null ? Integer.parseInt(params.get("port")) : null);
                this.requestParams = params.get("string");
            }
        }

        private void doAction(DataOutputStream out) throws IOException {
            switch (this.requestAction){
                case "ask":
                    doProxyRequest(out);
                    break;
                default:
                    out.writeBytes(createResponse("400 Bad Request", "Action " + this.requestAction + " not supported!"));
                    break;
            }
        }

        private void doProxyRequest(DataOutputStream out) throws IOException {
            String serverResponse;
            try{
                serverResponse = TCPClient.askServer(this.requestHost, this.requestPort, this.requestParams);
            } catch (Exception e) {
                serverResponse = "";
            }
            if(serverResponse == null || serverResponse.length() == 0){
                out.writeBytes(createResponse("404 Not Found", "Could not find what you where looking for!"));
            } else if(serverResponse.startsWith("HTTP")) {
                out.writeBytes(serverResponse);
            } else {
                out.writeBytes(createResponse("200 OK", serverResponse));
            }
        }

        private String createResponse(String statusCode, String data) {
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

            return "HTTP/1.1 " + statusCode + this.lineEnding +
                    "Server: HTTP Multithreaded Server/1.0 (java)" + this.lineEnding +
                    "Date: " + format.format(new java.util.Date()) + this.lineEnding +
                    "Content-Type: text/plain" + this.lineEnding +
                    "Content-Length: " + data.length() + this.lineEnding +
                    this.lineEnding +
                    data;
        }
    }
}

class TCPClient {

    static String askServer(String hostname, int port, String ToServer) throws IOException {
        Socket socket = setupSocket(hostname, port);

        if(ToServer != null) writeToServer(ToServer, socket);
        String serverResponse = readServerResponse(socket);

        tearDownSocket(socket);

        return serverResponse;
    }

    private static Socket setupSocket(String hostname, int port) throws IOException {
        Socket s = new Socket(hostname, port);
        s.setSoTimeout(2000);
        return s;
    }

    private static void writeToServer(String message, Socket s) throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(message));
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        String currentLine;

        while ((currentLine = br.readLine()) != null) {
            out.writeBytes(currentLine + "\n");
        }
    }

    private static String readServerResponse(Socket s) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        StringBuilder sb = new StringBuilder();
        int currentChar;

        try {
            while ((currentChar = br.read()) > 0) {sb.append((char) currentChar);}
        } catch (SocketTimeoutException ignored){}

        return sb.toString();
    }

    private static void tearDownSocket(Socket s) throws IOException {
        s.close();
    }
}

