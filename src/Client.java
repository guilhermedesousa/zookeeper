import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.lang.StringBuilder;

public class Client {
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
     * Send the client request to the specified server.
     *
     * @param IP the server IP
     * @param port the server port
     * @param message the message from the client
     * @return the server response
     */
    private Message send(String IP, int port, Message message) {
        try {
            Socket s = new Socket(IP, port);

            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());

            os.writeObject(message);

            Message response = (Message) is.readObject();

            s.close();

            return response;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform the PUT operation.
     *
     * @param key the key to insert
     * @param value the value to insert
     */
    private void put(String key, String value) {
        updateTimestamp();

        Message message = new Message(Message.Operation.PUT, key, value);
        message.setClientTimestamp(getTimestamp());

        String[] server = getRandomServer();
        String serverIP = server[0];
        int serverPort = Integer.parseInt(server[1]);

        Message response = send(serverIP, serverPort, message);

        if (response.getResponse() == Message.ResponseType.PUT_OK) {
            keyTimestamps.put(key, response.getServerTimestamp());
            printPUT(key, value, response.getServerTimestamp(), serverIP, serverPort);
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

    private void printGET(String key, String value, long clientTimestamp, long serverTimestamp, String IP, String port) {
        StringBuilder sb = new StringBuilder();

        sb.append("GET ");
        sb.append("key: ").append(key).append(" ");
        sb.append("value: ").append(value).append(" ");
        sb.append("obtido do servidor ").append(IP).append(":").append(port).append(", ");
        sb.append("meu timestamp ").append(clientTimestamp).append(" ");
        sb.append("do servidor ").append(serverTimestamp);

        System.out.println(sb.toString());
    }

    private void get(String key) {
        // updateTimestamp();

        // Message message = new Message("GET", key);
        // Long lastTimestamp = keyTimestamps.getOrDefault(key, 0L);
        // message.setClientTimestamp(lastTimestamp);

        // String[] server = getRandomServer();
        // String serverIP = server[0];
        // String serverPort = server[1];

        // Message.ResponseMessage response = send(serverIP, serverPort, message);
        // if (!response.getValue().equals("TRY_OTHER_SERVER_OR_LATER")) {
        //     keyTimestamps.put(key, response.getServerTimestamp());
        //     printGET(key, response.getValue(), response.getClientTimestamp(), response.getServerTimestamp(), serverIP, serverPort);
        // }
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

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                throw new IllegalArgumentException("Usage: java Client <IP>:<port> <IP>:<port> <IP>:<port>");
            }

            Client client = new Client();

            for (int i = 0; i < args.length; i++) {
                client.serverList.add(args[i].split(":"));
            }

            while (true) {
                String clientInput = getClientInput();

                if (clientInput.equalsIgnoreCase("exit")) {
                    System.out.println("Closing the client...");
                    break;
                }

                String[] inputParts = clientInput.split(" ");

                boolean isGET = inputParts.length == 1;
                boolean isPUT = inputParts.length == 2;

                if (isPUT) {
                    String key = inputParts[0];
                    String value = inputParts[1];
                    client.put(key, value);
                } else if (isGET) {
                    String key = inputParts[0];
                    client.get(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
