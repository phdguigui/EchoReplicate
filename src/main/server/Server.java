package server;

import common.EchoService;
import org.eclipse.paho.client.mqttv3.*;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Server extends UnicastRemoteObject implements EchoService, MqttCallback {

    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String MQTT_TOPIC = "replicacao/mensagens";

    private List<String> messages = new ArrayList<>();
    private boolean isMaster = false;
    private MqttClient mqttClient;
    private String rmiName;
    private UUID uuid = UUID.randomUUID(); // usado futuramente para eleição

    protected Server(String rmiName) throws RemoteException {
        super();
        this.rmiName = rmiName;

        try {
            mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId());
            mqttClient.setCallback(this);
            mqttClient.connect();
        } catch (MqttException e) {
            System.out.println("Erro ao conectar MQTT:");
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            // Testa se já existe um mestre
            EchoService master = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
            System.out.println("Outro mestre já registrado. Atuando como réplica.");
            isMaster = false;

            // Registrar como réplica com nome único (usamos UUID)
            String replicaName = "replica_" + uuid.toString();
            Naming.rebind("rmi://localhost:1099/" + replicaName, this);

            // Inscrever no tópico MQTT
            mqttClient.subscribe(MQTT_TOPIC);
            System.out.println("Inscrito no tópico MQTT como réplica.");

            try {
                List<String> historico = master.getListOfMsg();
                messages.clear();
                messages.addAll(historico);
                System.out.println("Histórico de mensagens sincronizado com o mestre.");
            } catch (RemoteException e) {
                System.out.println("Erro ao sincronizar histórico com o mestre:");
                e.printStackTrace();
            }

            if (!isMaster) {
                iniciarMonitoramentoDoMestre();
            }
        } catch (NotBoundException e) {
            // Nenhum mestre registrado — este servidor será o mestre
            try {
                Naming.rebind("rmi://localhost:1099/echo", this);
                isMaster = true;
                System.out.println("Nenhum mestre encontrado. Atuando como mestre.");
            } catch (Exception ex) {
                System.out.println("Erro ao registrar como mestre:");
                ex.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Erro inesperado ao verificar mestre:");
            e.printStackTrace();
        }
    }

    @Override
    public String echo(String msg) throws RemoteException {
        messages.add(msg);
        System.out.println((isMaster ? "[MESTRE]" : "[RÉPLICA]") + " Mensagem recebida: " + msg);

        if (isMaster) {
            // Publica para as réplicas
            try {
                mqttClient.publish(MQTT_TOPIC, new MqttMessage(msg.getBytes()));
                System.out.println("Mensagem publicada no tópico MQTT.");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        return (isMaster ? "Eco (mestre): " : "Eco (réplica): ") + msg;
    }

    @Override
    public List<String> getListOfMsg() throws RemoteException {
        return messages;
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    // Métodos MQTT (para réplica)
    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Conexão MQTT perdida.");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String msg = new String(message.getPayload());
        System.out.println("[RÉPLICA] Mensagem replicada recebida: " + msg);
        messages.add(msg);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    public static void main(String[] args) {
        try {
            Server server = new Server("echo");
            server.init();

            System.out.println("Servidor ativo. UUID: " + server.uuid + " / Papel: " + (server.isMaster ? "MESTRE" : "RÉPLICA"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarMonitoramentoDoMestre() {
        new Thread(() -> {
            while (!isMaster) {
                try {
                    //Thread.sleep(100);

                    EchoService master = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
                    if (!master.isAlive()) {
                        throw new RemoteException("Mestre não respondeu corretamente.");
                    }

                    //System.out.println("[RÉPLICA] Mestre está ativo.");

                } catch (Exception e) {
                    System.out.println("[RÉPLICA] Mestre falhou! Iniciando processo de eleição...");
                    iniciarEleicao();
                    break;
                }
            }
        }).start();
    }

    private void iniciarEleicao() {
        try {
            String[] nomes = Naming.list("rmi://localhost:1099");

            // Verifica se já existe um novo mestre rebindado
            try {
                EchoService mestre = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
                if (mestre.isAlive()) {
                    System.out.println("[RÉPLICA] Novo mestre já está ativo. Abortando eleição.");
                    iniciarMonitoramentoDoMestre();
                    return;
                }
            } catch (Exception ex) {
                // Mestre não está ativo, prossegue com a eleição
            }

            boolean menorUUIDAtivo = false;

            for (String nome : nomes) {
                if (nome.contains("replica_")) {
                    try {
                        UUID outroUUID = UUID.fromString(nome.substring(nome.indexOf("replica_") + 8));
                        if (!outroUUID.equals(uuid) && outroUUID.compareTo(uuid) < 0) {
                            EchoService replica = (EchoService) Naming.lookup(nome);
                            if (replica.isAlive()) {
                                menorUUIDAtivo = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignora falha ao acessar réplica
                    }
                }
            }

            if (!menorUUIDAtivo) {
                virarMestre();
            } else {
                System.out.println("[RÉPLICA] Outro servidor com UUID menor ainda está ativo. Aguardando...");

                new Thread(() -> {
                    try {
                        Thread.sleep(0);

                        // Verifica se um novo mestre foi eleito nesse intervalo
                        try {
                            EchoService mestre = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
                            if (mestre.isAlive()) {
                                System.out.println("[RÉPLICA] Novo mestre identificado após espera. Abortando nova tentativa de eleição.");
                                iniciarMonitoramentoDoMestre(); // <- reinicia o monitoramento
                                return;
                            }
                        } catch (Exception ignored) {
                            // Sem mestre ainda, continua o processo de eleição
                        }

                        iniciarEleicao(); // tenta novamente
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

        } catch (Exception e) {
            System.out.println("Erro durante eleição:");
            e.printStackTrace();
        }
    }

    private void virarMestre() {
        try {
            // Cancela inscrição no MQTT
            mqttClient.unsubscribe(MQTT_TOPIC);

            // Faz rebind como novo mestre
            Naming.rebind("rmi://localhost:1099/echo", this);
            isMaster = true;

            System.out.println("ELEIÇÃO: Este servidor agora é o novo MESTRE.");
        } catch (Exception e) {
            System.out.println("Erro ao assumir papel de mestre:");
            e.printStackTrace();
        }
    }

}
