import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.lang.StringBuilder;
import com.google.gson.Gson;

public class Client {
    private String clientIP;
    private int clientPort;
    private List<String[]> serverList;
    private long timestamp;
    private Map<String, Long> keyTimestamps;

    /**
     * Create an instance of Client.
     */
    public Client() {
        this.serverList = new ArrayList<>();
        this.timestamp = 0;
        this.keyTimestamps = new HashMap<>();
    }

    /**
     * Get the client timestamp.
     *
     * @return the client timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Update the client timestamp with the current time.
     */
    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the client IP address.
     *
     * @return the client IP address
     */
    public String getClientIP() {
        return clientIP;
    }

    /**
     * Get the client port.
     *
     * @return the client port
     */
    public int getClientPort() {
        return clientPort;
    }

    /**
     * Perform the PUT operation.
     *
     * @param key the key to insert
     * @param value the value to insert
     */
    private void put(String key, String value) {
        updateTimestamp();

        Message message = new Message(Message.Operation.PUT, key, value, clientIP, clientPort);
        message.setClientTimestamp(getTimestamp());

        String[] server = getRandomServer();
        String serverIP = server[0];
        int serverPort = Integer.parseInt(server[1]);

        sendPut(serverIP, serverPort, message);
    }

    /**
     * Perform the GET operation.
     *
     * @param key the key to search
     */
    private void get(String key) {
        updateTimestamp();

        Message message = new Message(Message.Operation.GET, key, clientIP, clientPort);
        long lastTimestamp = keyTimestamps.getOrDefault(key, 0L);
        message.setClientTimestamp(lastTimestamp);

        String[] server = getRandomServer();
        String serverIP = server[0];
        int serverPort = Integer.parseInt(server[1]);

        Message response = sendGet(serverIP, serverPort, message);

        if (response.getResponse() == Message.ResponseType.GET_OK) {
            keyTimestamps.put(key, response.getServerTimestamp());
            printGET(key, response.getValue(), getTimestamp(), response.getServerTimestamp(), serverIP, serverPort);
        } else if (response.getResponse() == Message.ResponseType.NULL) {
            printGET(key, "NULL", getTimestamp(), 0L, serverIP, serverPort);
        } else if (response.getResponse() == Message.ResponseType.TRY_OTHER_SERVER_OR_LATER) {
            printGET(key, "TRY_OTHER_SERVER_OR_LATER", getTimestamp(), response.getServerTimestamp(), serverIP, serverPort);
        }
    }

    /**
     * Send the client PUT request to the specified server.
     *
     * @param IP the server IP
     * @param port the server port
     * @param message the message to send
     */
    private void sendPut(String IP, int port, Message message) {
        try {
            Socket s = new Socket(IP, port);

            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

            writer.writeUTF(message.toJson());

            s.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send the client GET request to the specified server.
     *
     * @param IP the server IP
     * @param port the server port
     * @param message the message to send
     * @return the server response
     */
    private Message sendGet(String IP, int port, Message message) {
        try {
            Socket s = new Socket(IP, port);

            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);
            
            InputStream is = s.getInputStream();
            DataInputStream reader = new DataInputStream(is);
            
            writer.writeUTF(message.toJson());

            String responseJson = reader.readUTF();
            Message response = Message.fromJson(responseJson);

            s.close();

            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Print the response related to the PUT operation.
     *
     * @param key the key inserted
     * @param value the value inserted
     * @param serverTimestamp the timestamp registered on the server
     * @param IP the server IP address
     * @param port the server port
     */
    private void printPUT(String key, String value, long serverTimestamp, String IP, int port) {
        StringBuilder sb = new StringBuilder();

        sb.append("PUT_OK ");
        sb.append("key: ").append(key).append(" ");
        sb.append("value ").append(value).append(" ");
        sb.append("timestamp ").append(serverTimestamp).append(" ");
        sb.append("realizada no servidor ").append(IP).append(":").append(port);

        System.out.println(sb.toString());
    }

    /**
     * Print the response related to the GET operation.
     *
     * @param key the key searched
     * @param value the value retrieved
     * @param clientTimestamp the client timestamp
     * @param serverTimestamp the server timestamp
     * @param IP the server IP address
     * @param port the server port
     */
    private void printGET(String key, String value, long clientTimestamp, long serverTimestamp, String IP, int port) {
        StringBuilder sb = new StringBuilder();

        sb.append("GET ");
        sb.append("key: ").append(key).append(" ");
        sb.append("value: ").append(value).append(" ");
        sb.append("obtido do servidor ").append(IP).append(":").append(port).append(", ");
        sb.append("meu timestamp ").append(clientTimestamp).append(" ");
        sb.append("do servidor ").append(serverTimestamp);

        System.out.println(sb.toString());
    }

    /**
     * Get a random server from the server list.
     *
     * @return the random server
     */
    private String[] getRandomServer() {
        Random random = new Random();
        int randomIndex = random.nextInt(serverList.size());

        return serverList.get(randomIndex);
    }

    /**
     * Get the client input from the console.
     *
     * @return the input string
     * @throws IOException exception when reading from the console
     */
    private static String getClientInput() throws IOException {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(is);

        return reader.readLine();
    }

    /**
     * Set client Info.
     *
     * @param clientInfo the client info
     */
    private void setClientInfo(String clientInfo) {
        this.clientIP = clientInfo.split(":")[0];
        this.clientPort = Integer.parseInt(clientInfo.split(":")[1]);
    }

    /**
     * Start the message receiver from the server.
     */
    private void startMessageReceiver() {
        try(ServerSocket serverSocket = new ServerSocket(clientPort)) {
           while (true) {
                Socket s = serverSocket.accept();

                InputStream is = s.getInputStream();
                DataInputStream reader = new DataInputStream(is);

                String responseJson = reader.readUTF();
                Message response = Message.fromJson(responseJson);

                if (response.getResponse() == Message.ResponseType.PUT_OK) {
                    keyTimestamps.put(response.getKey(), response.getServerTimestamp());
                    printPUT(response.getKey(), response.getValue(), response.getServerTimestamp(), response.getServerIP(), response.getServerPort());
                }

                s.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            
            while (true) {
                String clientInput = getClientInput();

                if (clientInput.equalsIgnoreCase("exit")) {
                    System.out.println("Closing the client...");
                    break;
                }

                String[] inputParts = clientInput.split(" ");
                String operation = inputParts[0];

                if (operation.equals("INIT")) {
                    String clientInfo = inputParts[1];
                    String[] serverInfos = Arrays.copyOfRange(inputParts, 2, inputParts.length);

                    client.setClientInfo(clientInfo);

                    for (String server : serverInfos) {
                        client.serverList.add(server.split(":"));
                    }

                    Thread ClientThread = new Thread(() -> {
                        client.startMessageReceiver();
                    });
                    ClientThread.start();
                } else if (operation.equals("PUT")) {
                    String key = inputParts[1];
                    String value = inputParts[2];
                    client.put(key, value);
                } else if (operation.equals("GET")) {
                    String key = inputParts[1];
                    client.get(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
