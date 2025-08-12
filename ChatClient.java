import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Simple console chat client.
 * Usage: java ChatClient <server-ip> [port]
 * Default server-ip: localhost
 * Default port: 12345
 */
public class ChatClient {
    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ChatClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void start() {
        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner sc = new Scanner(System.in);

            // Listen thread to print server messages
            new Thread(() -> {
                String serverMsg;
                try {
                    while ((serverMsg = in.readLine()) != null) {
                        // special commands from server
                        if (serverMsg.startsWith("ENTERNAME")) {
                            System.out.print("Enter username: ");
                        } else if (serverMsg.startsWith("NAMEINUSE")) {
                            System.out.println("Username in use. Choose another: ");
                        } else if (serverMsg.startsWith("WELCOME")) {
                            System.out.println(serverMsg);
                        } else if (serverMsg.startsWith("USERS ")) {
                            System.out.println("Current users: " + serverMsg.substring(6));
                        } else {
                            System.out.println(serverMsg);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            // Handle user input and send to server
            while (true) {
                String input = sc.nextLine();
                if (input == null) break;
                out.println(input);
                out.flush();
                if (input.equalsIgnoreCase("/quit") || input.equalsIgnoreCase("/exit")) {
                    break;
                }
            }

            shutdown();
        } catch (IOException e) {
            System.err.println("Unable to connect: " + e.getMessage());
        }
    }

    private void shutdown() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        System.out.println("Client closed.");
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        new ChatClient(host, port).start();
    }
}

    
