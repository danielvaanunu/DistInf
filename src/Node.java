import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Node implements Runnable {
    private int id;
    private Map<Integer, Double[]> neighbors_map;
    private Double[][] adjacency_matrix;
    private int num_of_nodes;
    private Server server;
    private Queue<Thread> threads_list;
    private volatile boolean stop;
    private Set<Integer> msg;
    private volatile boolean finished;

    public Node(int id, int num_of_nodes) {
        this.id = id;
        this.neighbors_map = new HashMap<>();
        this.num_of_nodes = num_of_nodes;
        this.adjacency_matrix = initialize_matrix();
        this.threads_list = new ConcurrentLinkedDeque<>();
        this.stop=false;
        this.msg = new HashSet<Integer>();
        // finished means that this node got msg from all other nodes
        this.finished = false;

    }
    public void setStop(){
        this.stop = true;
    }
    public void setFinished(){
        this.finished = true;
    }
    public boolean getFinished(){
        return this.finished;
    }
    public Server getServer(){
        return this.server;
    }
    public boolean getStop(){
        return this.stop;
    }
    public void addToMsg(int node_id){
        this.msg.add(node_id);
    }
    public Set<Integer> getMsg(){
        return this.msg;
    }

    private Double[][] initialize_matrix() {
        Double[][] matrix = new Double[num_of_nodes][num_of_nodes];
        for (int i = 0; i < this.num_of_nodes; i++) {
            for (int j = 0; j < this.num_of_nodes; j++) matrix[i][j] = Double.parseDouble("-1");
        }
        return matrix;
    }
    public void setAdjacency_matrix(int index, double weight) {
        this.adjacency_matrix[this.id - 1][index - 1] = weight;
    }
    public void addToThreadsList(Thread t){
        this.threads_list.add(t);
    }

    public Map<Integer, Double[]> getNeighbors_map() {
        return this.neighbors_map;
    }

    public Double[][] getAdjacency_matrix() {
        return this.adjacency_matrix;
    }
    public int getNum_of_nodes(){
        return this.num_of_nodes;
    }

    public Queue<Thread> getThreads_list(){
        return this.threads_list;
    }

    public void setNeighbors_map(int neighbor_id, Double[] properties) {
        this.neighbors_map.put(neighbor_id, properties);
    }
    public void setAdjacencyRow(int nodeID, Double[] properties){
        this.adjacency_matrix[nodeID - 1] = properties;
    }

    public void setWeight(int neighbor_id, double weight) {
        // change neighbor_map
        this.neighbors_map.get(neighbor_id)[0] = weight;
        // change adjacency_matrix
        this.adjacency_matrix[this.id - 1][neighbor_id - 1] = weight;
    }

    public int get_id() {
        return this.id;
    }

    public void print_graph() throws FileNotFoundException {
        for (int i = 0; i < this.num_of_nodes; i++) {
            for (int j = 0; j < this.num_of_nodes; j++) {
                System.out.print(this.adjacency_matrix[i][j]);
                if (j != this.num_of_nodes - 1) {
                    System.out.print(", ");
                }
            }
            System.out.print("\n");
        }
    }
    public void open_server(){
        this.restartRun();
        this.server = new Server(this);
    }

    public void start() {
        this.run();
    }

    public void start_server(){
        this.server.start();
    }

    public void restartRun(){
        this.threads_list.clear();
        this.msg.clear();
        this.msg.add(this.id);
        // finished means that this node got msg from all other nodes
        this.finished = false;
    }

    @Override
    public void run() {
        Thread t = new Thread(() -> {
            try {
                this.addToThreadsList(Thread.currentThread());
                // send first round
                for (Map.Entry<Integer, Double[]> entry : this.neighbors_map.entrySet()) {
                    int send_port = entry.getValue()[1].intValue();
                        Socket send = new Socket("localhost", send_port);
                        PrintWriter writer = new PrintWriter(send.getOutputStream(), true);
                        Double[] my_adjacency_arr = this.adjacency_matrix[this.id - 1];
                        Pair<Integer, String> msg_to_send = new Pair<>(this.id, Arrays.toString(my_adjacency_arr));
                        writer.println(msg_to_send + ", [" + this.id);
                        send.close();
                        writer.close();
                }
            } catch (Exception ignored) {}
        });
        t.start();
    }
}
