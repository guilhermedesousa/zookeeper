import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.Gson;

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
                OutputStream os = node.getOutputStream();
                DataOutputStream writer = new DataOutputStream(os);

                InputStream is = node.getInputStream();
                DataInputStream reader = new DataInputStream(is);
                
                String messageJson = reader.readUTF();
                Message message = Message.fromJson(messageJson);
                
                if (message.getOperation() == Message.Operation.PUT) {
                    handlePut(message);
                } else if (message.getOperation() == Message.Operation.REPLICATION) {
                    Message response = handleReplication(message);
                    writer.writeUTF(response.toJson());
                } else if (message.getOperation() == Message.Operation.GET) {
                    Message response = handleGet(message);
                    writer.writeUTF(response.toJson());
                }
            } catch (IOException e) {
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
     * @param message the message from the client
     */
    private void handlePut(Message message) {
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
        } else {
            System.out.printf("Encaminhando PUT key:%s value:%s%n", key, value);
            forwardPutToLeader(message);
        }
    }

    /**
     * Handle the GET operation.
     *
     * @param message the message sent by the client
     * @return the response
     */
    private Message handleGet(Message message) {
        String key = message.getKey();
        String clientIP = message.getClientIP();
        int clientPort = message.getClientPort();
        long clientTimestamp = message.getClientTimestamp();
        String value = keyValueStore.get(key);

        StringBuilder sb = new StringBuilder();

        sb.append("Cliente ").append(clientIP).append(":").append(clientPort).append(" ");
        sb.append("GET key: ").append(key).append(" ");
        sb.append("ts: ").append(clientTimestamp).append(". ");

        Message response;

        if (value == null) {
            response = new Message(Message.ResponseType.NULL);
            sb.append("Meu ts é NAO_EXISTE").append(", ");
            sb.append("portanto devolvendo ");
            sb.append("NULL");
        } else {
            long serverTimestamp = timestamps.get(key);

            // Simula o cenário do TRY_OTHER_SERVER_OR_LATER
            if (key.equals("Kx")) {
                response = new Message(Message.ResponseType.TRY_OTHER_SERVER_OR_LATER);
                response.setServerTimestamp(999999);

                sb.append("Meu ts é ").append(serverTimestamp).append(", ");
                sb.append("portanto devolvendo ");
                sb.append("TRY_OTHER_SERVER_OR_LATER");
                
                return response;
            }

            if (serverTimestamp >= clientTimestamp) {
                response = new Message(Message.ResponseType.GET_OK);
                response.setValue(value);
                response.setServerTimestamp(serverTimestamp);

                sb.append("Meu ts é ").append(serverTimestamp).append(", ");
                sb.append("portanto devolvendo ");
                sb.append(value);
            } else {
                response = new Message(Message.ResponseType.TRY_OTHER_SERVER_OR_LATER);
                response.setServerTimestamp(serverTimestamp);
                
                sb.append("Meu ts é ").append(serverTimestamp).append(", ");
                sb.append("portanto devolvendo ");
                sb.append("TRY_OTHER_SERVER_OR_LATER");
            }
        }

        System.out.println(sb.toString());
        return response;
    }

    /**
     * Forward PUT operation to leader.
     *
     * @param message the message from the client
     */
    private void forwardPutToLeader(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        String clientIP = message.getClientIP();
        int clientPort = message.getClientPort();
        long timestamp = message.getClientTimestamp();

        try {
            Socket s = new Socket(leaderIP, leaderPort);
            
            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

            Message forwardedMessage = new Message(Message.Operation.PUT, key, value, clientIP, clientPort);
            forwardedMessage.setClientTimestamp(timestamp);

            writer.writeUTF(forwardedMessage.toJson());

            s.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replicate the information on other servers.
     *
     * @param message the message from the client
     */
    private void replicate(Message message) {
        String key = message.getKey();
        String value = message.getValue();
        String clientIP = message.getClientIP();
        int clientPort = message.getClientPort();
        long timestamp = message.getClientTimestamp();

        int numFollowers = followers.size();
        AtomicInteger responsesReceived = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numFollowers);

        for (String[] follower : followers) {
            String followerIP = follower[0];
            int followerPort = Integer.parseInt(follower[1]);

            executor.submit(new Runnable() {
                public void run() {
                    try {
                        Socket s = new Socket(followerIP, followerPort);
                        
                        OutputStream os = s.getOutputStream();
                        DataOutputStream writer = new DataOutputStream(os);

                        InputStream is = s.getInputStream();
                        DataInputStream reader = new DataInputStream(is);
                        
                        Message repMessage = new Message(Message.Operation.REPLICATION, key, value, clientIP, clientPort);
                        repMessage.setServerTimestamp(timestamp);

                        writer.writeUTF(repMessage.toJson());
                        
                        String responseJson = reader.readUTF();
                        Message response = Message.fromJson(responseJson);

                        if (response.getResponse() == Message.ResponseType.REPLICATION_OK) {
                            int currentResponses = responsesReceived.incrementAndGet();

                            if (currentResponses == numFollowers) {
                                System.out.printf("Enviando PUT_OK ao Cliente %s:%s da key:%s ts:%d%n", clientIP, clientPort, key, timestamp);
                                sendResponse(key, value, clientIP, clientPort, timestamp);
                            }
                        }

                        s.close();
                    } catch (IOException e) {
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

    /**
     * Send PUT_OK to the client.
     *
     * @param key the key inserted
     * @param value the value inserted
     * @param clientIP the client IP address
     * @param clientPort the client port
     * @param timestamp the timestamp associated to the key
     */
    private void sendResponse(String key, String value, String clientIP, int clientPort, long timestamp) {
        Message response = new Message(Message.ResponseType.PUT_OK);
        response.setKey(key);
        response.setValue(value);
        response.setServerIP(serverIP);
        response.setServerPort(serverPort);
        response.setServerTimestamp(timestamp);

        try {
            Socket s = new Socket(clientIP, clientPort);

            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

            writer.writeUTF(response.toJson());

            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }

    /**
     * Start the server to handle client requests.
     *
     * @param port the server port
     */
    private void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
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
        if (leaderPort == 10097) {
            followers.add(new String[]{"127.0.0.1", "10098"});
            followers.add(new String[]{"127.0.0.1", "10099"});
        } else if (leaderPort == 10098) {
            followers.add(new String[]{"127.0.0.1", "10097"});
            followers.add(new String[]{"127.0.0.1", "10099"});
        } else if (leaderPort == 10099) {
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
