import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private String serverIP;
    private int serverPort;
    private String leaderIP;
    private int leaderPort;
    private Map<String, String> keyValueStore;
    private Map<String, Long> timestamps;

    public class ServerServiceThread extends Thread {
        private Socket node = null;

        public ServerServiceThread(Socket node) {
            this.node = node;
        }

        public void run() {
            try {
                ObjectOutputStream os = new ObjectOutputStream(node.getOutputStream());
                ObjectInputStream is = new ObjectInputStream(node.getInputStream());

                Message message = (Message) is.readObject();

                if (message.getOperation().equals("PUT")) {
                    Message.ResponseMessage response = handlePut(message);
                    os.writeObject(response);
                } else if (message.getOperation().equals("GET")) {
                    handleGet(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Server(String server, String leader) {
        this.serverIP = server.split(":")[0];
        this.serverPort = Integer.parseInt(server.split(":")[1]);
        this.leaderIP = leader.split(":")[0];
        this.leaderPort = Integer.parseInt(leader.split(":")[1]);
        this.keyValueStore = new HashMap<>();
        this.timestamps = new HashMap<>();
    }

    private boolean isLeader() {
        return serverIP.equals(leaderIP) && serverPort == leaderPort;
    }

    private Message.ResponseMessage handlePut(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        long clientTimestamp = message.getClientTimestamp();

        if (isLeader()) {
            keyValueStore.put(key, value);
            timestamps.put(key, clientTimestamp);
            
            // TODO: replication

            return new Message.ResponseMessage("PUT_OK", clientTimestamp);
        } else {
            return forwardPutToLeader(key, value, clientTimestamp);
        }
    }

    private Message.ResponseMessage forwardPutToLeader(String key, String value, long clientTimestamp) {
        try {
            Socket s = new Socket(leaderIP, leaderPort);

            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());

            Message message = new Message("PUT", key, value);
            message.setClientTimestamp(clientTimestamp);

            os.writeObject(message);

            Message.ResponseMessage response = (Message.ResponseMessage) is.readObject();

            s.close();

            return response;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGet(Message message) {

    }

    private void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running at port " + port);

            while (true) {
                Socket node = serverSocket.accept();
    
                ServerServiceThread thread = new ServerServiceThread(node);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: java Server <IP>:<port> <IP>:<port>");
        }

        String serverInfo = args[0];
        String leaderInfo = args[1];

        Server server = new Server(serverInfo, leaderInfo);

        server.startServer(server.serverPort);
    }
}
