import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Server implements Runnable {
    // responsible for waiting for new msg
    private Node node;
    private Queue<ServerSocket> server_sockets;

    public Server(Node node) {
        this.node = node;
        this.server_sockets = new ConcurrentLinkedDeque<>();
        this.start();
    }
    public Queue<ServerSocket> getServer_sockets(){
        return this.server_sockets;
    }

    public void start() {
        this.run();
    }

    @Override
    public void run() {
        // needs to listen all the time to new msg from neighbors
        // when receiving- needs to create new thread that handles the connection
        for (Map.Entry<Integer, Double[]> entry : this.node.getNeighbors_map().entrySet()) {
            int listen_port = entry.getValue()[2].intValue();
            try {
                ServerSocket socket = new ServerSocket(listen_port);
                this.server_sockets.add(socket);
                Thread t = new Thread(() -> {
                    try {
                        this.node.addToThreadsList(Thread.currentThread());
                        while (true) {
                            Socket client_socket = socket.accept();
                            Scanner input = new Scanner(client_socket.getInputStream());
                            String str_input = input.nextLine();
                            ClientHandler client_handler = new ClientHandler(this.node, str_input);
                            client_socket.close();
                            client_handler.start();
                        }
                    } catch (IOException ignored) {}
                    finally {
                        try {
                            socket.close();
                        } catch (IOException ignored) {}

                    }
                });
                t.start();
            } catch (Exception ignored) {}
        }
    }
}


