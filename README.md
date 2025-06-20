# 🔁 EchoReplicate

A distributed replicated echo service with passive replication and fault tolerance using indirect communication architecture (MQTT Broker).

---

## 📝 Project Overview

**EchoReplicate** is an academic project implementing a distributed remote echo service with passive replication and failure tolerance. The system is built on a client/server model with multiple servers (master and replicas/clones). It leverages indirect communication using an MQTT Broker for message replication among servers.

---

## 💡 System Concept

- **Echo Service:**  
  Clients can invoke the `echo(msg)` operation on the master server, which returns the message as an echo and replicates it to all server replicas.
- **Message History:**  
  Clients can invoke `getListOfMsg()` on any server to obtain the complete history of echoed messages.
- **Passive Replication:**  
  The master replicates all received messages to the replicas via MQTT publish/subscribe, ensuring all servers maintain a consistent state.
- **Fault Tolerance/Election:**  
  If the master fails, a new master is elected among the replicas. The election and failover are transparent to the client.
- **Dynamic Membership:**  
  New servers can join the system, subscribe to the MQTT topic, retrieve the current message history, and participate in leader election.

---

## 🏗️ Architecture

- **Client/Server with Multiple Servers:**  
  - Only one master at a time handles client requests.
  - All servers (master + clones) maintain the same message history.
- **Indirect Communication:**  
  - Replication uses MQTT Broker (e.g., Mosquitto) to propagate messages.
  - Master publishes new messages; replicas subscribe to the topic and update their local state.
- **Leader Election:**  
  - On master failure, replicas coordinate to elect a new master.
  - The new master unsubscribes from the MQTT topic to avoid duplicate processing.
  - Clients automatically redirect requests to the new master.

---

## ⚙️ Technologies Used

- **Java RMI** (preferred) or **Python (Pyro4)** for remote method invocation and inter-process communication.
- **MQTT Broker** (e.g., Mosquitto) for indirect message replication (publish/subscribe).
- **Eclipse Paho Java Client** for MQTT integration with Java (recommended).

---

## ▶️ How to Run

1. **Start the MQTT Broker**  
   (e.g., Mosquitto on Ubuntu 20.04 or your local environment.)

2. **Start Replica Servers**  
   - Launch one or more server instances (the same code, different processes).
   - Each server will register itself, subscribe to the MQTT topic, and sync replicated messages.

3. **Start the Client**  
   - Use the client interface to send `echo(msg)` or request `getListOfMsg()`.

4. **Simulate Failure and Election**  
   - Stop the master server process.
   - The system will automatically elect a new master, and clients will transparently redirect their requests.

---

## 📁 Repository Structure

- `src/` - Source code (servers, clients, utilities).
- `docs/` - Documentation and UML diagrams.
- `scripts/` - Helper scripts for running multiple servers/clients.

---

## 🌱 Next Steps / Improvements

- Distributed (rather than centralized) leader election.
- History resynchronization for late-joining replicas.
- Enhanced failure detection and monitoring.

---

## 🤝 Contributions

Pull requests, issues, and suggestions are welcome!  
Feel free to fork this repository and contribute.

---

## 👤 Author

Developed by Guilherme Siedschlag  
[GitHub Profile](https://github.com/phdguigui)

---

> **Disclaimer:** Academic project for Distributed Systems coursework (Prof. Adriano Fiorese).
