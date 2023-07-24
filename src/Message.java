import com.google.gson.Gson;

public class Message {
    enum Operation {
        PUT,
        GET,
        REPLICATION
    }

    enum ResponseType {
        PUT_OK,
        GET_OK,
        REPLICATION_OK,
        NULL,
        TRY_OTHER_SERVER_OR_LATER
    }

    private Operation operation;
    private String key;
    private String value;
    private String clientIP;
    private int clientPort;
    private String serverIP;
    private int serverPort;
    private ResponseType response;
    private long clientTimestamp;
    protected long serverTimestamp;

    public Message() {}

    /**
     * Create an instance of Message for PUT and REPLICATION operations.
     *
     * @param operation PUT
     * @param key the key to insert
     * @param value the value to insert
     * @param clientIP the client IP address
     * @param clientPort the client port
     */
    public Message(Operation operation, String key, String value, String clientIP, int clientPort) {
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
    }

    /**
     * Create an instance of Message for GET operation.
     *
     * @param operation GET
     * @param key the key to request
     * @param clientIP the client IP address
     * @param clientPort the client port
     */
    public Message(Operation operation, String key, String clientIP, int clientPort) {
        this.operation = operation;
        this.key = key;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
    }

    /**
     * Create an instance of Message for response operation.
     *
     * @param response the response type
     */
    public Message(ResponseType response) {
        this.response = response;
    }

    /**
     * Convert the Message object to a JSON string.
     *
     * @return the JSON representation of the Message
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Convert a JSON string to a Message object.
     *
     * @param json the JSON representation of the Message
     * @return the Message object
     */
    public static Message fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Message.class);
    }
    
    /**
     * Get the response
     * @return the response type
     */
    public ResponseType getResponse() {
        return response;
    }

    /**
     * Get the operation
     *
     * @return the operation type
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Get the key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the key.
     *
     * @param key the key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Get the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /*
     * Set the value.
     */
    public void setValue(String value) {
        this.value = value;
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
     * Get the server IP address.
     *
     * @return the server IP address
     */
    public String getServerIP() {
        return serverIP;
    }

    /**
     * Set the server IP address.
     *
     * @param serverIP the server IP address
     */
    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    /**
     * Get the server port.
     *
     * @return the server port
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Set the server port.
     *
     * @param serverPort the server port
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Get the client timestamp.
     *
     * @return the client timestamp
     */
    public long getClientTimestamp() {
        return clientTimestamp;
    }

    /**
     * Set the client timestamp.
     *
     * @param clientTimestamp the client timestamp
     */
    public void setClientTimestamp(long clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    /**
     * Get the server timestamp.
     *
     * @return the server timestamp
     */
    public long getServerTimestamp() {
        return this.serverTimestamp;
    }

    /**
     * Set the server timestamp.
     *
     * @param serverTimestamp the server timestamp
     */
    public void setServerTimestamp(long serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}