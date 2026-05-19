package client;

import java.io.*;
import java.net.*;

public class ClientNetwork {

    private final String ip;
    private final int port;
    private final String username;
    private final ClientGUI gui;

    private Socket socket;
    private PrintWriter out;
    private Thread listenerThread;
    private volatile boolean running = false;

    public ClientNetwork(String ip, int port, String username, ClientGUI gui) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.gui = gui;
    }

    public boolean connect() {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Login isteği gönder
            out.println("LOGIN:" + username);

            // Sunucudan gelen mesajları dinle
            listenerThread = new Thread(this::listenFromServer);
            running = true;
            listenerThread.start();

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    private void listenFromServer() {
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            String line;
            while (running && (line = in.readLine()) != null) {
                handleIncoming(line);
            }

        } catch (IOException e) {
            if (running) {
                gui.appendMessage("[System] Connection to server lost.");
            }
        }
    }

    private void handleIncoming(String line) {
        if (line.startsWith("MESSAGE:")) {
            // FORMAT: MESSAGE:username:content
            String[] parts = line.split(":", 3);
            if (parts.length == 3) {
                gui.appendMessage("[" + parts[1] + "]: " + parts[2]);
            }

        } else if (line.startsWith("SYSTEM:")) {
            gui.appendMessage("[System] " + line.substring(7));

        } else if (line.startsWith("USERLIST:")) {
            // FORMAT: USERLIST:user1,user2,user3
            String data = line.substring(9);
            String[] users = data.split(",");
            gui.updateUserList(users);

        } else if (line.startsWith("KICKED:")) {
            running = false;
            gui.onKicked();
        }
    }

    public void sendMessage(String text) {
        if (out != null) {
            out.println("MESSAGE:" + username + ":" + text);
        }
    }

    public void disconnect() {
        running = false;
        if (out != null) {
            out.println("LOGOUT:" + username);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // socket already closed
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
