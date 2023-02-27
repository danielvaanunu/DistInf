import java.net.ServerSocket;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class ExManager {
    private String path;
    private int num_of_nodes;
    private Map<Integer, Node> nodes_map;
    private int counter;


    public ExManager(String path) {
        this.path = path;
        this.nodes_map = new HashMap<>();
        this.counter = 0;

    }

    public Node get_node(int id) {
        return this.nodes_map.get(id);
    }

    public int getNum_of_nodes() {
        return this.num_of_nodes;
    }

    public void update_edge(int id1, int id2, double weight) {
        this.nodes_map.get(id1).setWeight(id2, weight);
        this.nodes_map.get(id2).setWeight(id1, weight);
    }

    public void read_txt() {
        int line_counter = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(this.path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("stop")) {
                    break;
                }
                if (line_counter == 0) {
                    this.num_of_nodes = Integer.parseInt(line);
                } else {
                    String[] line_arr = line.split(" ");
                    Double[] properties;
                    int neighbor_id, node_id;
                    double weight_neighbor;
                    node_id = Integer.parseInt(line_arr[0]);
                    Node node = new Node(node_id, this.num_of_nodes);
                    for (int i = 1; i < line_arr.length; i += 4) {
                        properties = new Double[3];
                        neighbor_id = Integer.parseInt(line_arr[i]);
                        weight_neighbor = Double.parseDouble(line_arr[i + 1]);
                        properties[0] = weight_neighbor;
                        properties[1] = Double.parseDouble(line_arr[i + 2]);
                        properties[2] = Double.parseDouble(line_arr[i + 3]);

                        node.setNeighbors_map(neighbor_id, properties);

                        node.setAdjacency_matrix(neighbor_id, weight_neighbor);
                    }
                    this.nodes_map.put(node_id, node);
                }
                line_counter += 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws InterruptedException, IOException {
        if (this.num_of_nodes >= 80) {
                TimeUnit.SECONDS.sleep(150);
        } else {
                TimeUnit.SECONDS.sleep(120);
        }
        for (int i = 1; i <= this.num_of_nodes; i++) {
            this.nodes_map.get(i).open_server();
        }
        for (int i = 1; i <= this.num_of_nodes; i++) {
            this.nodes_map.get(i).start();
        }
        boolean temp = false;
        int count;
        while (!temp) {
            count = 0;
            for (int i = 1; i <= this.num_of_nodes; i++) {
                if (!this.nodes_map.get(i).getFinished()) {
                    break;
                } else {
                    count = count + 1;
                }
            }
            if (count == this.num_of_nodes) {
                temp = true;
            }
        }
        if (temp) {
            for (int i = 1; i <= this.num_of_nodes; i++) {
                for (ServerSocket server : this.nodes_map.get(i).getServer().getServer_sockets()) {
                        server.close();
                }
                for (Thread thread : this.nodes_map.get(i).getThreads_list()) {
                        thread.join(1000);
                }
            }
        }
        this.terminate();

        this.counter += 1;
    }
    public void terminate() {
        for (int i = 1; i <= this.num_of_nodes; i++) {
            Queue<Thread> threadsQueue = this.nodes_map.get(i).getThreads_list();
            for (Thread thread1 : threadsQueue) {
                thread1.interrupt();
            }
        }
        for (int i = 1; i <= this.num_of_nodes; i++) {
            this.nodes_map.get(i).restartRun();
        }
    }
}
