package server;

import common.EchoService;
import org.eclipse.paho.client.mqttv3.*;
import entities.Echo;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.*;

public class Server implements MqttCallback {

    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String MQTT_TOPIC = "replicacao/mensagens";

    private boolean isMaster = false;
    private MqttClient mqttClient;
    private Echo echoObject;
    private UUID uuid = UUID.randomUUID();

    public Server() {
        try {
            mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId());
            mqttClient.setCallback(this);
            mqttClient.connect();
        } catch (MqttException e) {
            System.out.println("Erro ao conectar ao broker MQTT:");
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            // Verifica se já há mestre
            EchoService mestre = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
            System.out.println("Outro mestre já registrado. Atuando como réplica.");
            isMaster = false;

            echoObject = new Echo(false, mqttClient, null);

            // Sincroniza mensagens do mestre
            try {
                List<String> historico = mestre.getListOfMsg();
                echoObject.sincronizarMensagens(historico);
            } catch (Exception e) {
                System.out.println("Erro ao sincronizar histórico com mestre:");
                e.printStackTrace();
            }

            // Registrar réplica com nome único
            String replicaName = "replica_" + uuid;
            Naming.rebind("rmi://localhost:1099/" + replicaName, echoObject);

            mqttClient.subscribe(MQTT_TOPIC);
            System.out.println("Inscrito no tópico MQTT como réplica.");

            iniciarMonitoramentoDoMestre();

        } catch (NotBoundException e) {
            // Nenhum mestre: torna-se o mestre
            try {
                isMaster = true;
                echoObject = new Echo(true, mqttClient, null);
                Naming.rebind("rmi://localhost:1099/echo", echoObject);
                System.out.println("Nenhum mestre encontrado. Atuando como mestre.");
            } catch (Exception ex) {
                System.out.println("Erro ao registrar como mestre:");
                ex.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Erro inesperado ao verificar mestre:");
            e.printStackTrace();
        }

        System.out.println("Servidor ativo. UUID: " + uuid + " / Papel: " + (isMaster ? "MESTRE" : "RÉPLICA"));
    }

    private void iniciarMonitoramentoDoMestre() {
        new Thread(() -> {
            while (!isMaster) {
                try {
                    EchoService mestre = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
                    if (!mestre.isAlive()) throw new Exception("Mestre não respondeu.");
                } catch (Exception e) {
                    System.out.println("[RÉPLICA] Mestre falhou! Iniciando processo de eleição...");
                    iniciarEleicao();
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    private void iniciarEleicao() {
        try {
            String[] nomes = Naming.list("rmi://localhost:1099");

            // Verifica se já existe novo mestre
            try {
                EchoService mestre = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
                if (mestre.isAlive()) {
                    System.out.println("[RÉPLICA] Novo mestre já está ativo. Abortando eleição.");
                    iniciarMonitoramentoDoMestre();
                    return;
                }
            } catch (Exception ignored) {}

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
                    } catch (Exception ignored) {}
                }
            }

            if (!menorUUIDAtivo) {
                virarMestre();
            } else {
                System.out.println("[RÉPLICA] Outro servidor com UUID menor ainda está ativo. Aguardando...");

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        try {
                            EchoService mestre = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
                            if (mestre.isAlive()) {
                                System.out.println("[RÉPLICA] Novo mestre identificado após espera. Abortando nova tentativa de eleição.");
                                iniciarMonitoramentoDoMestre();
                                return;
                            }
                        } catch (Exception ignored) {}

                        iniciarEleicao();
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
            mqttClient.unsubscribe(MQTT_TOPIC);

            echoObject = new Echo(true, mqttClient, echoObject != null ? echoObject.getListOfMsg() : null);

            Naming.rebind("rmi://localhost:1099/echo", echoObject);
            isMaster = true;

            System.out.println("ELEIÇÃO: Este servidor agora é o novo MESTRE.");
        } catch (Exception e) {
            System.out.println("Erro ao assumir papel de mestre:");
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Conexão MQTT perdida.");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String msg = new String(message.getPayload());
        if (!isMaster && echoObject != null) {
            echoObject.adicionarMensagem(msg);
            System.out.println("[RÉPLICA] Mensagem replicada recebida: " + msg);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    public static void main(String[] args) {
        new Server().init();
    }
}
