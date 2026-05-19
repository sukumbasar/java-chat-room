package server;

public class WorkerThread implements Runnable {

    private final String request;
    private final ServerGUI serverGUI;

    public WorkerThread(String request, ServerGUI serverGUI) {
        this.request = request;
        this.serverGUI = serverGUI;
    }

    @Override
    public void run() {
        if (request == null || !request.contains(":")) return;

        String[] parts = request.split(":", 3);
        String type = parts[0];
        String username = parts.length > 1 ? parts[1] : "";
        String content = parts.length > 2 ? parts[2] : "";

        switch (type) {
            case "LOGIN" -> handleLogin(username);
            case "MESSAGE" -> handleMessage(username, content);
            case "LOGOUT" -> handleLogout(username);
            case "KICK" -> handleKick(username);
            case "STOP" -> handleStop();
            default -> serverGUI.logMessage("Unknown request: " + request);
        }
    }

    private void handleLogin(String username) {
        serverGUI.addUser(username);
        serverGUI.broadcast("SYSTEM:" + "User " + username + " has joined the chat");
        serverGUI.broadcastUserList();
        serverGUI.logMessage("User connected: " + username);
    }

    private void handleMessage(String username, String content) {
        serverGUI.broadcast("MESSAGE:" + username + ":" + content);
        serverGUI.logMessage("[" + username + "]: " + content);
    }

    private void handleLogout(String username) {
        serverGUI.removeUser(username);
        serverGUI.broadcast("SYSTEM:" + "User " + username + " has left the chat");
        serverGUI.broadcastUserList();
        serverGUI.logMessage("User disconnected: " + username);
    }

    private void handleKick(String username) {
        serverGUI.kickUser(username);
        serverGUI.broadcast("SYSTEM:" + "User " + username + " was removed by the server");
        serverGUI.broadcastUserList();
        serverGUI.logMessage("User kicked: " + username);
    }

    private void handleStop() {
        serverGUI.broadcast("SYSTEM:Server is shutting down...");
        serverGUI.stopServer();
    }
}
