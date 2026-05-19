package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerGUI extends JFrame {

    private JTextField portField;
    private JButton startButton, stopButton, kickButton;
    private JTextArea chatArea, logArea;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    private ServerSocket serverSocket;
    private final SharedBuffer sharedBuffer = new SharedBuffer(100);
    private final Map<String, ClientHandler> clients = Collections.synchronizedMap(new LinkedHashMap<>());
    private Dispatcher dispatcher;
    private Thread dispatcherThread;
    private volatile boolean serverRunning = false;

    public ServerGUI() {
        setTitle("Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));

        // --- Top: port + buttons ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("5000", 6);
        topPanel.add(portField);
        startButton = new JButton("Start Server");
        stopButton  = new JButton("Stop Server");
        kickButton  = new JButton("Kick Selected User");
        stopButton.setEnabled(false);
        topPanel.add(startButton);
        topPanel.add(stopButton);
        topPanel.add(kickButton);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: chat area ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Chat Messages"));

        // --- Right: user list ---
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(160, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Active Users"));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
        centerSplit.setResizeWeight(0.75);
        add(centerSplit, BorderLayout.CENTER);

        // --- Bottom: log area ---
        logArea = new JTextArea(6, 0);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Server Log"));
        add(logScroll, BorderLayout.SOUTH);

        // --- Listeners ---
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> {
            try {
                sharedBuffer.produce("STOP:server:");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        kickButton.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected != null) {
                try {
                    sharedBuffer.produce("KICK:" + selected + ":");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a user to kick.");
            }
        });
    }

    private void startServer() {
        String portText = portField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.");
            return;
        }

        serverRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        portField.setEnabled(false);

        dispatcher = new Dispatcher(sharedBuffer, this);
        dispatcherThread = new Thread(dispatcher);
        dispatcherThread.start();

        int finalPort = port;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(finalPort);
                logMessage("Server started on port " + finalPort);
                while (serverRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(clientSocket, sharedBuffer, this);
                        new Thread(handler).start();
                        logMessage("New connection from: " + clientSocket.getInetAddress());
                    } catch (IOException ex) {
                        if (serverRunning) logMessage("Connection error: " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                logMessage("Could not start server: " + e.getMessage());
            }
        }).start();
    }

    public void stopServer() {
        serverRunning = false;
        dispatcher.stop();
        dispatcherThread.interrupt();

        synchronized (clients) {
            for (ClientHandler ch : clients.values()) {
                ch.disconnect();
            }
            clients.clear();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logMessage("Error closing server: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            portField.setEnabled(true);
            logMessage("Server stopped.");
        });
    }

    public void addUser(String username) {
        SwingUtilities.invokeLater(() -> {
            if (!userListModel.contains(username)) {
                userListModel.addElement(username);
            }
        });
    }

    public void removeUser(String username) {
        SwingUtilities.invokeLater(() -> userListModel.removeElement(username));
        synchronized (clients) {
            clients.remove(username);
        }
    }

    public void kickUser(String username) {
        ClientHandler handler;
        synchronized (clients) {
            handler = clients.get(username);
        }
        if (handler != null) {
            handler.getOut().println("KICKED:You have been removed by the server.");
            handler.disconnect();
            removeUser(username);
        }
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler ch : clients.values()) {
                ch.getOut().println(message);
            }
        }
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("SYSTEM:")) {
                chatArea.append("[SYSTEM] " + message.substring(7) + "\n");
            } else if (message.startsWith("MESSAGE:")) {
                String[] parts = message.split(":", 3);
                if (parts.length == 3) {
                    chatArea.append("[" + parts[1] + "]: " + parts[2] + "\n");
                }
            }
        });
    }

    public void broadcastUserList() {
        StringBuilder sb = new StringBuilder("USERLIST:");
        synchronized (clients) {
            sb.append(String.join(",", clients.keySet()));
        }
        String msg = sb.toString();
        synchronized (clients) {
            for (ClientHandler ch : clients.values()) {
                ch.getOut().println(msg);
            }
        }
    }

    public void registerClient(String username, ClientHandler handler) {
        synchronized (clients) {
            clients.put(username, handler);
        }
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}