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

To run the application, follow the steps below. All scripts mentioned are located inside the `scripts/` folder, so **you must execute the commands from inside this folder**.

## ✨ Execution Permissions

If you get a permission error when executing a script, make all of them executable with:

```bash
chmod +x *.sh
```

---

## ① `./build.sh`

Compiles all `.java` files inside the `src/main/` folder, including using the MQTT JAR dependency. The compiled `.class` files are generated inside the `run/` folder.

> Use this script whenever you modify any code.

---

## ② `./run_registry.sh`

Starts the **RMI Registry**, which is required so remote objects can be found and accessed.

- This process runs in the background.
- To stop it, use the following command (or Ctrl C in the terminal that is running the process):

```bash
pkill rmiregistry
```

---

## ③ `./run_server.sh`

Starts a server instance:

- If no master is registered, the server will become the **master**.
- If a master already exists, the server will act as a **replica**.

> You can execute this script in multiple terminals to simulate multiple replicas/servers.

---

## ④ `./run_client.sh`

Starts a client that communicates with the current master server.

The client allows you to:

- Send messages (echo)
- View message history
- Test fault tolerance and master re-election

> You can run it as many times as needed, even simultaneously.

---

## ⑤ `./clean.sh`

Removes all `.class` files and clears the `run/` folder, resetting the project to its initial state.

> Useful to ensure a clean build or prepare the environment before delivery/testing.

---

## ✅ Recommended Execution Order

1. `./build.sh` – compile all sources
2. `./run_registry.sh` – start the RMI Registry
3. `./run_server.sh` – run in multiple terminals to start servers/replicas
4. `./run_client.sh` – run to test interaction

You can re-run `run_server.sh` to add more replicas at any time.

Finish with `clean.sh` to reset the environment if wanted.

---

## 📁 Repository Structure

- `src/` - Source code (servers, clients, utilities).
- `docs/` - Documentation and UML diagrams.
- `scripts/` - Helper scripts for running multiple servers/clients.

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
