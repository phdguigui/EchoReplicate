package entities;

import common.EchoService;
import org.eclipse.paho.client.mqttv3.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Echo extends UnicastRemoteObject implements EchoService {

    private List<String> messages = new ArrayList<>();
    private final boolean isMaster;
    private final MqttClient mqttClient;

    public Echo(boolean isMaster, MqttClient mqttClient, List<String> mensagensAnteriores) throws RemoteException {
        this.isMaster = isMaster;
        this.mqttClient = mqttClient;
        this.messages = mensagensAnteriores != null ? new ArrayList<>(mensagensAnteriores) : new ArrayList<>();
    }

    @Override
    public String echo(String msg) throws RemoteException {
        messages.add(msg);
        System.out.println((isMaster ? "[MESTRE]" : "[RÉPLICA]") + " Mensagem recebida: " + msg);

        if (isMaster && mqttClient != null) {
            try {
                mqttClient.publish("replicacao/mensagens", new MqttMessage(msg.getBytes()));
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

    public void sincronizarMensagens(List<String> mensagensExistentes) {
        messages.clear();
        messages.addAll(mensagensExistentes);
    }

    public void adicionarMensagem(String message){
        messages.add(message);
    }
}
