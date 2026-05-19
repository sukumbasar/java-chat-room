package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ClientGUI extends JFrame {

    // Login components
    private JPanel loginPanel;
    private JTextField ipField, portField, usernameField;
    private JButton connectButton;

    // Chat components
    private JPanel chatPanel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, exitButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    private ClientNetwork network;

    public ClientGUI() {
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        buildLoginPanel();
        buildChatPanel();
        showLogin();
        setVisible(true);
    }

    // ─── Login Panel ─────────────────────────────────────────────
    private void buildLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Chat Room Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(title, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Server IP:"), gbc);
        ipField = new JTextField("127.0.0.1", 16);
        gbc.gridx = 1;
        loginPanel.add(ipField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        loginPanel.add(new JLabel("Port:"), gbc);
        portField = new JTextField("5000", 16);
        gbc.gridx = 1;
        loginPanel.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        loginPanel.add(new JLabel("Username:"), gbc);
        usernameField = new JTextField(16);
        gbc.gridx = 1;
        loginPanel.add(usernameField, gbc);

        connectButton = new JButton("Connect");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        loginPanel.add(connectButton, gbc);

        connectButton.addActionListener(e -> handleConnect());

        // Enter tuşu ile de bağlanabilsin
        usernameField.addActionListener(e -> handleConnect());
    }

    // ─── Chat Panel ──────────────────────────────────────────────
    private void buildChatPanel() {
        chatPanel = new JPanel(new BorderLayout(8, 8));

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Messages"));

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Active Users"));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
        centerSplit.setResizeWeight(0.78);
        chatPanel.add(centerSplit, BorderLayout.CENTER);

        // Bottom: message input
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 0));
        messageField = new JTextField();
        sendButton = new JButton("Send");
        exitButton = new JButton("Exit");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.add(sendButton);
        btnPanel.add(exitButton);
        bottomPanel.add(btnPanel, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        exitButton.addActionListener(e -> handleExit());
    }

    // ─── Panel switching ─────────────────────────────────────────
    private void showLogin() {
        setContentPane(loginPanel);
        revalidate();
        repaint();
    }

    private void showChat() {
        setContentPane(chatPanel);
        revalidate();
        repaint();
    }

    // ─── Actions ─────────────────────────────────────────────────
    private void handleConnect() {
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();
        String username = usernameField.getText().trim();

        if (ip.isEmpty() || portText.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.");
            return;
        }

        network = new ClientNetwork(ip, port, username, this);
        boolean connected = network.connect();

        if (connected) {
            setTitle("Chat Client — " + username);
            showChat();
        } else {
            JOptionPane.showMessageDialog(this, "Could not connect to server.");
        }
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty() && network != null) {
            network.sendMessage(text);
            messageField.setText("");
        }
    }

    private void handleExit() {
        if (network != null) {
            network.disconnect();
        }
        System.exit(0);
    }

    // ─── Called by ClientNetwork ──────────────────────────────────
    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String u : users) {
                if (!u.isEmpty()) userListModel.addElement(u);
            }
        });
    }

    public void onKicked() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "You have been removed by the server.");
            showLogin();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
