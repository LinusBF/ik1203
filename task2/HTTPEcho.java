import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;

public class HTTPEcho {
    private ServerSocket serverSocket;
    public static void main(String[] args) throws IOException {
        HTTPEcho server = new HTTPEcho(Integer.parseInt(args[0]));
        server.startServer();
    }

    private HTTPEcho(int port) throws IOException {
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


        Worker(Socket socket, String lineEnding) {
            this.socket = socket;
            this.lineEnding = lineEnding;
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
            String line;
            while ((line = bf.readLine()) != null && !line.equals("")) {
                sb.append(line).append(this.lineEnding);
            }
            out.writeBytes(createResponse(sb.toString()));
        }

        private String createResponse(String data) {
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

            return "HTTP/1.1 200 OK" + this.lineEnding +
                    "Server: HTTP Multithreaded Server/1.0 (java)" + this.lineEnding +
                    "Date: " + format.format(new java.util.Date()) + this.lineEnding +
                    "Content-Type: text/plain" + this.lineEnding +
                    "Content-Length: " + data.length() + this.lineEnding +
                    this.lineEnding +
                    data;
        }
    }
}

