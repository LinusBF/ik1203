package tcpclient;

import java.net.*;
import java.io.*;

public class TCPClient {

    public static String askServer(String hostname, int port, String ToServer) throws IOException {
        Socket socket = setupSocket(hostname, port);

        if(ToServer != null) writeToServer(ToServer, socket);
        String serverResponse = readServerResponse(socket);

        tearDownSocket(socket);

        return serverResponse;
    }

    public static String askServer(String hostname, int port) throws IOException {
        return askServer(hostname, port, null);
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
        String currentLine;
        try {
            while ((currentLine = br.readLine()) != null) {sb.append(currentLine).append("\n");}
        } catch (SocketTimeoutException ignored){}

        return sb.toString();
    }

    private static void tearDownSocket(Socket s) throws IOException {
        s.close();
    }
}
