package net.salesianos.server.threads;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class ClientHandler extends Thread {

  private DataInputStream clientInputStream;
  private String name;
  private ArrayList<DataOutputStream> clientsOutputs;

  public ClientHandler(DataInputStream clientInputStream, String name,
      ArrayList<DataOutputStream> clientsOutputsStream) {
    this.clientInputStream = clientInputStream;
    this.name = name;
    this.clientsOutputs = clientsOutputsStream;
  }

  @Override
  public void run() {
    try {
      while (true) {
        String receivedMessage = this.name + ": " + clientInputStream.readUTF();
        System.out.println(receivedMessage);

        for (DataOutputStream dataOutputStream : clientsOutputs) {
          dataOutputStream.writeUTF(receivedMessage);
          dataOutputStream.flush();
        }
      }
    } catch (SocketException se) {
      System.out.println("Conexión cerrada con cliente " + this.name + ".");
    } catch (IOException ioe) {
      System.out.println("Input output exception.");
    }
  }
}
