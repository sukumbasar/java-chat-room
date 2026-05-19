package server;

public class Dispatcher implements Runnable {

    private final SharedBuffer sharedBuffer;
    private final ServerGUI serverGUI;
    private volatile boolean running = true;

    public Dispatcher(SharedBuffer sharedBuffer, ServerGUI serverGUI) {
        this.sharedBuffer = sharedBuffer;
        this.serverGUI = serverGUI;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                String request = sharedBuffer.consume();
                WorkerThread worker = new WorkerThread(request, serverGUI);
                new Thread(worker).start();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}