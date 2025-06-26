package client;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;
import common.EchoService;

public class Client {

    private static EchoService echoService;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (echoService == null) {
            tentarConectar();
        }

        while (true) {
            System.out.println("\n1 - Enviar mensagem");
            System.out.println("2 - Ver histórico");
            System.out.println("3 - Sair");
            System.out.print("Escolha: ");
            int opcao = Integer.parseInt(scanner.nextLine());

            switch (opcao) {
                case 1:
                    System.out.print("Mensagem: ");
                    String msg = scanner.nextLine();
                    String resposta = echo(msg);
                    System.out.println("Resposta: " + resposta);
                    break;
                case 2:
                    List<String> mensagens = buscarHistorico();
                    System.out.println("Histórico:");
                    for (String m : mensagens) {
                        System.out.println("- " + m);
                    }
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static String echo(String msg) {
        while (true) {
            try {
                return echoService.echo(msg);
            } catch (RemoteException e) {
                reconectar();
            }
        }
    }

    private static List<String> buscarHistorico() {
        while (true) {
            try {
                return echoService.getListOfMsg();
            } catch (RemoteException e) {
                reconectar();
            }
        }
    }

    private static void reconectar() {
        echoService = null;
        while (echoService == null) {
            tentarConectar();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static void tentarConectar() {
        try {
            echoService = (EchoService) Naming.lookup("rmi://localhost:1099/echo");
        } catch (Exception e) {
        }
    }
}
