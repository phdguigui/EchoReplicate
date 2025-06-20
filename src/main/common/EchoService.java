package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface EchoService extends Remote {
    String echo(String msg) throws RemoteException;
    List<String> getListOfMsg() throws RemoteException;
    boolean isAlive() throws RemoteException;
}
