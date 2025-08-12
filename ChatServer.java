import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Multi-threaded chat server.
 * Usage: java ChatServer [port]
 * Default port: 12345
 */
public class ChatServer {
    private final int port;
    // username -> writer for that client
    private final ConcurrentMap<String, PrintWriter> clients = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("Chat server starting on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // handle client in separate thread
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcast(String fromUser, String message) {
        String full = fromUser + ": " + message;
        clients.forEach((user, writer) -> {
            // don't send to closed writers
            try {
                writer.println(full);
                writer.flush();
            } catch (Exception e) {
                // ignore - will be cleaned on disconnect
            }
        });
    }

    private void addClient(String username, PrintWriter writer) {
        clients.put(username, writer);
        broadcast("SERVER", username + " has joined the chat.");
    }

    private void removeClient(String username) {
        if (username != null && clients.remove(username) != null) {
            broadcast("SERVER", username + " has left the chat.");
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket.getRemoteSocketAddress());
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Ask for a username
                out.println("ENTERNAME");
                username = in.readLine();
                if (username == null) {
                    closeEverything();
                    return;
                }

                // If username exists, force the client to choose another
                while (username.trim().isEmpty() || clients.containsKey(username)) {
                    out.println("NAMEINUSE");
                    username = in.readLine();
                    if (username == null) {
                        closeEverything();
                        return;
                    }
                }

                // join
                addClient(username, out);
                out.println("WELCOME " + username);
                out.println("USERS " + String.join(",", clients.keySet()));

                // read messages
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) {
                        break;
                    }
                    // simple broadcast
                    broadcast(username, line);
                }
            } catch (IOException e) {
                System.err.println("I/O for client failed: " + e.getMessage());
            } finally {
                removeClient(username);
                closeEverything();
            }
        }

        private void closeEverything() {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
            System.out.println("Connection closed: " + socket.getRemoteSocketAddress());
        }
    }

    public static void main(String[] args) {
        int port = 12345;
        if (args.length >= 1) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        new ChatServer(port).start();
    }
}
