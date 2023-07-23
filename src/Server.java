import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private String serverIP;
    private int serverPort;
    private String leaderIP;
    private int leaderPort;
    private ConcurrentHashMap<String, String> keyValueStore;
    private ConcurrentHashMap<String, Long> timestamps;
    private List<String[]> followers = new ArrayList<>();

    /**
     * Thread the handle client requests.
     */
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
                
                if (message.getOperation() == Message.Operation.PUT) {
                    Message response = handlePut(message);
                    os.writeObject(response);
                } else if (message.getOperation() == Message.Operation.REPLICATION) {
                    Message response = handleReplication(message);
                    os.writeObject(response);
                }

                // if (message.getOperation().equals("PUT")) {
                //     System.out.println("PUT at port " + serverPort);
                //     Message.ResponseMessage response = handlePut(message);
                //     os.writeObject(response);
                // } else if (message.getOperation().equals("REPLICATION")) {
                //     Message.ResponseMessage response = handleReplication(message);
                //     os.writeObject(response);
                // } else if (message.getOperation().equals("GET")) {
                //     Message.ResponseMessage response = (Message.ResponseMessage) handleGet(message);
                //     os.writeObject(response);
                // }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create an instance of Server.
     *
     * @param server the server info
     * @param leader the leader info
     */
    public Server(String server, String leader) {
        this.serverIP = server.split(":")[0];
        this.serverPort = Integer.parseInt(server.split(":")[1]);
        this.leaderIP = leader.split(":")[0];
        this.leaderPort = Integer.parseInt(leader.split(":")[1]);
        this.keyValueStore = new ConcurrentHashMap<>();
        this.timestamps = new ConcurrentHashMap<>();

        if (isLeader()) {
            setFollowers();
        }
    }

    /**
     * Handle the PUT operation.
     *
     * @param message the message containing the pair key-value
     * @return PUT_OK
     */
    private Message handlePut(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        String clientIP = message.getClientIP();
        int clientPort = message.getClientPort();
        long timestamp = message.getClientTimestamp();

        if (isLeader()) {
            System.out.printf("Cliente %s:%s PUT key:%s value:%s%n", clientIP, clientPort, key, value);

            keyValueStore.put(key, value);
            timestamps.put(key, timestamp);
            
            replicate(message);
            
            System.out.printf("Enviando PUT_OK ao Cliente %s:%s da key:%s ts:%d%n", clientIP, clientPort, key, timestamp);

            Message response = new Message(Message.ResponseType.PUT_OK);
            response.setServerTimestamp(timestamp);

            return response;
        } else {
            System.out.printf("Encaminhando PUT key:%s value:%s%n", key, value);
            return forwardPutToLeader(message);
        }
    }

    /**
     * Forward PUT operation to leader.
     *
     * @param key the key to insert
     * @param value the value to insert
     * @param timestamp the timestamp
     * @return PUT_OK
     */
    private Message forwardPutToLeader(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        String clientIP = message.getClientIP();
        int clientPort = message.getClientPort();
        long timestamp = message.getClientTimestamp();

        try {
            Socket s = new Socket(leaderIP, leaderPort);

            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());

            Message forwardedMessage = new Message(Message.Operation.PUT, key, value, clientIP, clientPort);
            forwardedMessage.setClientTimestamp(timestamp);

            os.writeObject(forwardedMessage);

            Message response = (Message) is.readObject();

            s.close();

            return response;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replicate the information on other servers.
     *
     * @param key the key to insert
     * @param value the value to insert
     * @param timestamp the associated timestamp
     */
    private void replicate(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        String clientIP = message.getClientIP();
        int clientPort = message.getClientPort();
        long timestamp = message.getClientTimestamp();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (String[] follower : followers) {
            String followerIP = follower[0];
            int followerPort = Integer.parseInt(follower[1]);

            executor.submit(new Runnable() {
                public void run() {
                    try {
                        Socket s = new Socket(followerIP, followerPort);

                        ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
                        ObjectInputStream is = new ObjectInputStream(s.getInputStream());
                        
                        Message repMessage = new Message(Message.Operation.REPLICATION, key, value, clientIP, clientPort);
                        repMessage.setServerTimestamp(timestamp);
                        os.writeObject(repMessage);

                        Message response = (Message) is.readObject();

                        s.close();
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        executor.shutdown();

        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle the REPLICATION operation.
     *
     * @param message the message
     * @return REPLICATION_OK
     */
    private Message handleReplication(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        long timestamp = message.getServerTimestamp();

        System.out.printf("REPLICATION key:%s value:%s ts:%d%n", key, value, timestamp);

        keyValueStore.put(key, value);
        timestamps.put(key, timestamp);

        return new Message(Message.ResponseType.REPLICATION_OK);
    }

    // private Message handleGet(Message message) {
    //     String key = message.getKey();
    //     String value = keyValueStore.get(key);

    //     Message response;

    //     if (value == null) {
    //         response = new Message("NULL");
    //     } else if (timestamps.get(key) >= message.getClientTimestamp()) {
    //         response = new Message(value);
    //     } else {
    //         response = new Message("TRY_OTHER_SERVER_OR_LATER");
    //     }

    //     return response;
    // }

    /**
     * Start the server to handle client requests.
     *
     * @param port the server port
     */
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

    /**
     * Get the current server IP address.
     *
     * @return the server IP address.
     */
    public String getServerIP() {
        return this.serverIP;
    }

    /**
     * Get the current server port.
     *
     * @return the server port
     */
    public int getServerPort() {
        return this.serverPort;
    }

    /**
     * Set leader followers.
     */
    private void setFollowers() {
        if (serverPort == 10097) {
            followers.add(new String[]{"127.0.0.1", "10098"});
            followers.add(new String[]{"127.0.0.1", "10099"});
        } else if (serverPort == 10098) {
            followers.add(new String[]{"127.0.0.1", "10097"});
            followers.add(new String[]{"127.0.0.1", "10099"});
        } else if (serverPort == 10099) {
            followers.add(new String[]{"127.0.0.1", "10097"});
            followers.add(new String[]{"127.0.0.1", "10098"});
        }
    }

    /**
     * Check if the current server is a leader.
     *
     * @return true if it is a leader, false otherwise
     */
    private boolean isLeader() {
        return serverIP.equals(leaderIP) && serverPort == leaderPort;
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
