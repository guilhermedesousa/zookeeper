import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.lang.StringBuilder;

public class Client {
    List<String[]> serverList;
    private long timestamp;

    public Client() {
        this.serverList = new ArrayList<>();
        this.timestamp = 0;
    }

    private Message.ResponseMessage send(String IP, String port, Message message) {
        try {
            Socket s = new Socket(IP, Integer.parseInt(port));

            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());

            os.writeObject(message);

            Message.ResponseMessage response = (Message.ResponseMessage) is.readObject();

            return response;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void printPUT(String key, String value, long serverTimestamp, String IP, String port) {
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

    private void put(String key, String value) {
        updateTimestamp();

        Message message = new Message(Operation.PUT, key, value);
        message.setClientTimestamp(getTimestamp());

        String[] server = getRandomServer();
        String serverIP = server[0];
        String serverPort = server[1];

        Message.ResponseMessage response = send(serverIP, serverPort, message);
        printPUT(key, value, response.getServerTimestamp(), serverIP, serverPort);
    }

    private void get(String key) {
        updateTimestamp();

        Message message = new Message(Operation.GET, key);
        message.setClientTimestamp(getTimestamp());

        String[] server = getRandomServer();
        String serverIP = server[0];
        String serverPort = server[1];

        Message.ResponseMessage response = send(serverIP, serverPort, message);
        printGET(key, response.getValue(), response.getClientTimestamp(), response.getServerTimestamp(), serverIP, serverPort);
    }

    private String[] getRandomServer() {
        Random random = new Random();
        int randomIndex = random.nextInt(serverList.size());

        return serverList.get(randomIndex);
    }

    private static String getClientInput() throws IOException {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(is);

        return reader.readLine();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        try {
            if (args.length < 6) {
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

                boolean isPUT = inputParts.length > 1;
                boolean isGET = inputParts.length == 1;

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
