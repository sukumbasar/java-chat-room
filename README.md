# 💬 Java Network Chat Room

A real-time multi-client chat room application built with **Java Socket Programming**, **Multithreading**, **Synchronization**, and the **Producer–Consumer design pattern**. Both the server and client applications feature fully graphical interfaces built with **Java Swing**.

---

## ✨ Features

- 🔌 Multiple clients can connect to the server simultaneously
- 💬 Real-time message broadcasting to all connected clients
- 👥 Live active users list updated for all clients
- 🚫 Admin can kick any user from the server
- 🛑 Admin can stop the server and gracefully disconnect all clients
- 🔒 Thread-safe shared bounded buffer (Producer–Consumer pattern)
- 🖥️ Full Java Swing GUI — no console I/O

---

## 🏗️ Project Structure

```
java-chat-room/
├── ChatServer/
│   └── src/server/
│       ├── ServerGUI.java        # Main server window (entry point)
│       ├── ClientHandler.java    # One thread per client — Producer
│       ├── SharedBuffer.java     # Single bounded buffer (capacity: 100)
│       ├── Dispatcher.java       # Consumer thread — spawns WorkerThreads
│       └── WorkerThread.java     # Processes each request and broadcasts
│
└── ChatClient/
    └── src/client/
        ├── ClientGUI.java        # Login screen + Chat interface (entry point)
        └── ClientNetwork.java    # TCP socket connection and message I/O
```

---

## ⚙️ Concurrency Design

The server implements the **Producer–Consumer pattern** as follows:

```
Client sends request
       ↓
ClientHandler (Producer) → sharedBuffer.produce(request)
       ↓
     [ SharedBuffer — capacity: 100 — synchronized + wait/notifyAll ]
       ↓
Dispatcher (Consumer) → sharedBuffer.consume()
       ↓
new WorkerThread(request).start()
       ↓
WorkerThread processes → broadcasts to all clients
```

### Thread roles

| Thread | Role | Responsibility |
|---|---|---|
| `ClientHandler` | Producer | Reads from client socket, inserts into SharedBuffer |
| `Dispatcher` | Consumer | Reads from SharedBuffer, creates WorkerThreads |
| `WorkerThread` | Worker | Processes one request (LOGIN, MESSAGE, LOGOUT, KICK, STOP) |
| `ClientNetwork listener` | Client-side | Listens for server messages on a dedicated thread |

---

## 🔒 Synchronization

All shared resources are properly synchronized:

- **SharedBuffer** — `synchronized` methods + `wait()` / `notifyAll()`
- **Active users map** — `synchronized(clients)` block on every access
- **Broadcast operations** — `synchronized(clients)` during iteration
- **GUI updates** — always via `SwingUtilities.invokeLater()`

---

## 📡 Message Protocol

All messages use a simple colon-separated text protocol:

```
TYPE:username:content
```

| Type | Direction | Description |
|---|---|---|
| `LOGIN:username` | Client → Server | User connects |
| `MESSAGE:username:text` | Client → Server | Chat message |
| `LOGOUT:username` | Client → Server | User exits |
| `KICK:username:` | Server internal | Admin kicks user |
| `STOP:server:` | Server internal | Admin stops server |
| `SYSTEM:text` | Server → Client | System notification |
| `USERLIST:u1,u2,...` | Server → Client | Updated user list |
| `KICKED:reason` | Server → Client | Kicked notification |

---

## 🚀 How to Run

### Requirements
- Java JDK 17 or later
- Apache NetBeans 29 or later
- No external libraries required

### 1. Start the Server
1. Open `ChatServer` project in NetBeans
2. Select the project and press **F6**
3. Enter a port number (e.g. `5001`)
4. Click **Start Server**
5. Server log will show: `Server started on port 5001`

### 2. Start a Client
1. Open `ChatClient` project in NetBeans
2. Select the project and press **F6**
3. Enter server IP (`127.0.0.1` for local), port, and username
4. Click **Connect**

> 💡 To test with multiple clients, press **F6** again on ChatClient. Each client must have a unique username.

