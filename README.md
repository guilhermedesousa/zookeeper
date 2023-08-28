<div align="center">
<h1>ZooKeeper</h1>
</div>

This project involves a distributed system that stores key-value pairs in a replicated and consistent way.

The project is a simple implementation of Apache ZooKeeper, which is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services.

***

### Comunication

The communication between different components of the system is done via Transmission Control Protocol (TCP), transmitting messages in Gson format over the network.

***

### Architecture

The architecture of the system consists of three servers and multiple clients, where clients start requests for insertion, update or retrieval of information.

**1. Server**

Each server is responsible for storing the key-value pair information from clients that perform PUT requests. Additionally, a timestamp is associated with each inserted key.

There is also a special server, known as the leader, which handle all PUT requests and subsequently replicates this information to its followers (other servers). The followers, in turn, need to store the IP address and port of the leader server to forward these requests.

**1.1 Data structure**

The storage of the key-value pairs is made using a Java Map, specifically employing a thread-safe implementation of the pattern, namely ConcurrentHashMap. It is justified by the fact that multiple threads can modify the same data structure simultaneously, as seen with the server receiving concurrent PUT requests from clients. As a result, ConcurrentHashMap ensures that these operations are performed atomically.

**2. Client**

The clients are entities capable to request key-value information insertion (PUT) on any server, as well as to extract values based on the provided key in the request (GET). Furthermore, each client has its own timestamp, stored in the `timestamp` property, and it also maintains the last timestamp associated with each key using a map, with the property called `keyTimestamps`.

**Threads**

To make the system scalable and prevent process from becoming blocked, threads were employed on it.

The clients should be capable of receiving `PUT_OK` acknowlegments from the server simultaneously, and to achieve this, a thread called `ClientThread` is created. This separated thread is responsible for listening to requests on the client's port, reading the data stream, and displaying PUT information on the console when the response is of type `PUT_OK`.

As requested, each server should be capable to handle concurrent requests efficiently. To facilitate this, the `startServer` method processes incoming requests by generating individual `ServerServiceThread` threads for each one. Furthermore, when replicating data to the leader's followers, a new thread is created to manage the requests. To achieve this, a Java interface named `ExecutorService` was employed, providing an organized and abstract way to perform tasks within threads. Tha varible `executor` functions as a thread pool, where submitted tasks are managed and executed.

***

### Compilation

The compilation of the source code should be done in the terminal, compiling all the .java files simultaneously. Here's the suggested compilation code:

```
javac -d ..\bin -cp .;..\gson-2.10.1.jar Client.java Message.java Server.java
```

***

### Console operations

**Execution and server initialization**

```
java -cp .;..\gson-2.10.1.jar Server <IP>:<port> <leader IP>:<leader port>
```

**Client execution**

```
java -cp .;..\gson-2.10.1.jar Client
```

**INIT operation in the Client console**

```
INIT <client IP>:<client port> <server IP 1>:<server port 1> <server IP 2>:<server port 2> <server IP 3>:<server port 3>
```

**PUT operation in the Client console**

```
PUT <key> <value>
```

**GET operation in the Client console**

```
GET <key>
```