package net.salesianos.client.threads;

import java.io.DataInputStream;
import java.io.IOException;
import net.salesianos.utils.CryptoUtils;

public class ServerListener extends Thread {

  private DataInputStream inputStream;

  public ServerListener(DataInputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public void run() {
    while (true) {
      try {
        String encrypted = this.inputStream.readUTF();
        String fullMessage = CryptoUtils.decrypt(encrypted);
        String[] parts = fullMessage.split("\\|", 2);
        String type = parts[0];
        String msg = parts[1];

        switch (type) {
          case "chat":
          case "system":
          case "question":
          case "timer":
            System.out.println(msg);
            break;
        }

        if (type.equals("timer") || type.equals("question")) {
          System.out.print("-> ");
        }

      } catch (IOException e) {
        System.out.println("Conexión cerrada con el servidor.");
        break;
      } catch (Exception e) {
        System.out.println("Error al descifrar mensaje del servidor: " + e.getMessage());
        break;
      }
    }
  }
}