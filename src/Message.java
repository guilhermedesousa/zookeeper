import java.io.Serializable;

enum Operation {
    PUT,
    GET,
    PUT_OK
}

public class Message implements Serializable {
    private Operation operation;
    private String key;
    private String value;
    private long clientTimestamp;
    protected long serverTimestamp;

    public static class ResponseMessage extends Message {
        public ResponseMessage(Operation operation, long serverTimestamp) {
            super(operation);
            setServerTimestamp(serverTimestamp);
        }
    }

    /**
     * Create an instance of Message for GET operation
     *
     * @param operation GET
     * @param key the key to request
     */
    public Message(Operation operation, String key) {
        this.operation = operation;
        this.key = key;
        this.value = null;
    }

    /**
     * Create an instance of Message for PUT operation
     *
     * @param operation PUT
     * @param key the key to store
     * @param value the value to store
     */
    public Message(Operation operation, String key, String value) {
        this.operation = operation;
        this.key = key;
        this.value = value;
    }

    public Message(Operation operation) {
        this.operation = operation;
    }

    public Message(String value) {
        this.value = value;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public long getClientTimestamp() {
        return clientTimestamp;
    }

    public void setClientTimestamp(long clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    public long getServerTimestamp() {
        return this.serverTimestamp;
    }

    public void setServerTimestamp(long serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}