package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final SharedBuffer sharedBuffer;
    private final ServerGUI serverGUI;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, SharedBuffer sharedBuffer, ServerGUI serverGUI) {
        this.socket = socket;
        this.sharedBuffer = sharedBuffer;
        this.serverGUI = serverGUI;
    }

    public String getUsername() {
        return username;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("LOGIN:")) {
                    username = line.substring(6);
                    serverGUI.registerClient(username, this);
                }
                sharedBuffer.produce(line);
            }

        } catch (IOException | InterruptedException e) {
            if (username != null) {
                try {
                    sharedBuffer.produce("LOGOUT:" + username);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            // socket already closed
        }
    }
}
