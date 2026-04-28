package net.salesianos.server;

import java.io.*;
import java.net.*;
import java.util.*;
import net.salesianos.utils.Constants;
import net.salesianos.utils.QuestionLoader;

public class ServerApp {
  private static final int MAX_ROUNDS = 5;
  private static final List<DataOutputStream> clientsOutputs = new ArrayList<>();
  private static final List<String> clientNames = new ArrayList<>();
  private static final Map<String, Integer> scores = new HashMap<>();
  public static volatile boolean gameStarted = false;
  public static volatile boolean acceptingAnswers = false;
  private static String currentAnswer = null;
  private static final Object answerLock = new Object();

  private static String[][] questions;

  public static void main(String[] args) throws IOException {
    questions = QuestionLoader.loadQuestionsFromJson();

    ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
    System.out.println("Servidor levantado en el puerto " + serverSocket.getLocalPort());

    Thread waitingThread = new Thread(() -> {
      while (!gameStarted) {
        synchronized (clientsOutputs) {
          if (clientNames.size() < 2) {
            System.out.println("Esperando jugadores...");
          }
        }
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          break;
        }
      }
    });
    waitingThread.setDaemon(true);
    waitingThread.start();

    while (true) {
      Socket clientSocket = serverSocket.accept();
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
      DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
      String name = in.readUTF();

      synchronized (clientsOutputs) {
        clientsOutputs.add(out);
        clientNames.add(name);
        scores.put(name, 0);
        System.out.println(name + " se ha conectado. Jugadores: " + clientNames.size());
        if (clientNames.size() >= 2 && !gameStarted) {
          broadcast("Esperando más jugadores... si quieren empezar, escriban 'start'.");
        }
      }

      ClientHandler handler = new ClientHandler(in, out, name);
      handler.start();
    }
  }

  static synchronized boolean attemptStart(String name) {
    if (!gameStarted && clientNames.size() >= 2) {
      gameStarted = true;
      broadcast("¡Comienza el juego! " + name + " ha iniciado la partida.");
      new Thread(ServerApp::gameLoop).start();
      return true;
    } else if (clientNames.size() < 2) {
      try {
        for (int i = 0; i < clientNames.size(); i++) {
          if (clientNames.get(i).equals(name)) {
            clientsOutputs.get(i).writeUTF("Se necesitan al menos 2 jugadores.");
            clientsOutputs.get(i).flush();
            break;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  static void broadcast(String msg) {
    synchronized (clientsOutputs) {
      for (DataOutputStream out : clientsOutputs) {
        try {
          out.writeUTF(msg);
          out.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  static void submitAnswer(String name, String answer) {
    synchronized (answerLock) {
      if (acceptingAnswers && currentAnswer != null) {
        if (answer.toLowerCase().equals(currentAnswer.toLowerCase())) {
          int newScore = scores.get(name) + 1;
          scores.put(name, newScore);
          broadcast(name + " ha acertado! Respuesta: " + currentAnswer + " (+1 punto)");
          acceptingAnswers = false;
          currentAnswer = null;
          answerLock.notifyAll();
        }
      }
    }
  }

  static void removeClient(String name, DataOutputStream out) {
    synchronized (clientsOutputs) {
      clientsOutputs.remove(out);
      clientNames.remove(name);
      scores.remove(name);
    }
  }

  private static void gameLoop() {
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < questions.length; i++)
      indices.add(i);
    Collections.shuffle(indices);

    int roundCount = 0;
    for (int idx : indices) {
      if (roundCount >= MAX_ROUNDS)
        break;

      synchronized (clientsOutputs) {
        if (clientNames.size() < 2) {
          broadcast("No hay suficientes jugadores. Juego terminado.");
          break;
        }
      }

      roundCount++;
      String q = questions[idx][0];
      String a = questions[idx][1];

      broadcast("--- Ronda " + roundCount + " ---");
      broadcast("Pregunta: " + q);

      synchronized (answerLock) {
        acceptingAnswers = true;
        currentAnswer = a;
        try {
          answerLock.wait(30000);
        } catch (InterruptedException e) {
          break;
        }

        if (acceptingAnswers && currentAnswer != null) {
          broadcast("¡Tiempo! La respuesta era: " + currentAnswer);
        }
        acceptingAnswers = false;
      }

      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        break;
      }
    }

    gameStarted = false;
    broadcast("=== JUEGO TERMINADO ===");
    broadcast("Clasificación final:");

    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
    sorted.sort((e1, e2) -> e2.getValue() - e1.getValue());

    int pos = 1;
    for (Map.Entry<String, Integer> entry : sorted) {
      broadcast(pos + ". " + entry.getKey() + " - " + entry.getValue() + " puntos");
      pos++;
    }

    scores.clear();
    for (String name : clientNames) {
      scores.put(name, 0);
    }
  }
}