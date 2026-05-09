package net.salesianos.server.threads;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import net.salesianos.server.ServerApp;
import net.salesianos.utils.CryptoUtils;

public class ClientHandler extends Thread {

  private DataInputStream clientInputStream;
  private DataOutputStream clientOutputStream;
  private String name;

  public ClientHandler(DataInputStream clientInputStream, DataOutputStream clientOutputStream, String name) {
    this.clientInputStream = clientInputStream;
    this.clientOutputStream = clientOutputStream;
    this.name = name;
  }

  @Override
  public void run() {
    try {
      while (true) {
        String encrypted = clientInputStream.readUTF();
        String message = CryptoUtils.decrypt(encrypted);

        if (ServerApp.gameStarted && ServerApp.acceptingAnswers) {
          ServerApp.submitAnswer(this.name, message);
        } else if (!ServerApp.gameStarted && message.trim().toLowerCase().equals("start")) {
          ServerApp.attemptStart(this.name);
        } else if (ServerApp.chatMode) {
          ServerApp.handleChat(this.name, message);
        }
      }
    } catch (SocketException se) {
      System.out.println("Conexión cerrada con cliente " + this.name + ".");
    } catch (IOException ioe) {
      System.out.println("Error con cliente " + this.name + ".");
    } catch (Exception e) {
      System.out.println("Error al descifrar mensaje de " + this.name + ": " + e.getMessage());
    }
  }
}