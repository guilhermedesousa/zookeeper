import java.io.Serializable;

public class Message implements Serializable {
    enum Operation {
        PUT,
        GET,
        REPLICATION
    }

    enum ResponseType {
        PUT_OK,
        REPLICATION_OK
    }

    private Operation operation;
    private String key;
    private String value;
    private String clientIP;
    private int clientPort;
    private ResponseType response;
    private long clientTimestamp;
    protected long serverTimestamp;

    /**
     * Create an instance of Message for PUT and REPLICATION operations.
     *
     * @param operation PUT
     * @param key the key to insert
     * @param value the value to insert
     */
    public Message(Operation operation, String key, String value, String clientIP, int clientPort) {
        this.operation = operation;
        this.key = key;
        this.value = value;
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
     * Create an instance of Message for GET operation
     *
     * @param operation GET
     * @param key the key to request
     */
    public Message(String operation, String key) {
        // this.operation = operation;
        // this.key = key;
        // this.value = null;
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
     * Get the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
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