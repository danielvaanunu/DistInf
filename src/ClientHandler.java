import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class ClientHandler implements Runnable {
    // responsible for sending msg to neighbors
    private Node node;
    private String msg_to_forward;
    private int rec_id;
    private Double[] rec_properties;
    private final java.util.concurrent.locks.ReentrantLock lock;
    private int neighbor_id;

    public ClientHandler(Node node, String msg_to_forward) {
        this.node = node;
        this.msg_to_forward = msg_to_forward;
        this.lock = new ReentrantLock();
        this.rec_id = 0;
        this.rec_properties = new Double[this.node.getNum_of_nodes()];
        this.neighbor_id = 0;
    }

    public void decode_msg(String msg) {
        String[] items = msg.split(", \\[");
        // rec_id is how the matrix row is
        this.rec_id = Integer.parseInt(items[0].substring(1));
        String temp = items[1].substring(0, items[1].length() - 2);
        String[] temp_prop = temp.split(", ");
        this.rec_properties = Arrays.stream(temp_prop).mapToDouble(Double::parseDouble).boxed().toArray(Double[]::new);
        // neighbor_id is my neighbor who sent this msg
        this.neighbor_id = Integer.parseInt(items[2]);
    }

    public void start() {
        this.run();
    }

    @Override
    public void run() {
        this.decode_msg(this.msg_to_forward);
        // do the changes to my matrix
        try {
            this.lock.lock();
            this.node.setAdjacencyRow(this.rec_id, this.rec_properties);
        } finally {
            this.lock.unlock();
        }
        if (!this.node.getFinished() && !this.node.getMsg().contains(this.rec_id)) {
            for (Map.Entry<Integer, Double[]> entry : this.node.getNeighbors_map().entrySet()) {
                int neighbor_id = entry.getKey();
                if (neighbor_id != this.neighbor_id) {
                    int send_port = entry.getValue()[1].intValue();
                    // create new thread to send message to neighbor
                    Thread t3 = new Thread(() -> {
                        try {
                            this.node.addToThreadsList(Thread.currentThread());
                            Socket send = new Socket("localhost", send_port);
                            PrintWriter writer = new PrintWriter(send.getOutputStream(), true);
                            Pair<Integer, String> msg_to_forward = new Pair<>(this.rec_id, Arrays.toString(this.rec_properties));
                            writer.println(msg_to_forward + ", [" + this.node.get_id());
                            send.close();
                            writer.close();

                        } catch (Exception ignored) {}
                    });
                    t3.start();
                }
            }
        }
        try {
            this.lock.lock();
            this.node.addToMsg(this.rec_id);
            if (this.node.getMsg().size() == this.node.getNum_of_nodes()) {
                this.node.setFinished();
            }
        } finally {
            this.lock.unlock();
        }
    }
}
