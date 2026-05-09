package net.salesianos.server;

import java.io.*;
import java.net.*;
import java.util.*;
import net.salesianos.server.threads.ClientHandler;
import net.salesianos.utils.Constants;
import net.salesianos.utils.CryptoUtils;
import net.salesianos.utils.QuestionLoader;

public class ServerApp {
  private static final int MAX_ROUNDS = 5;
  private static final int ANSWER_TIME = 30;
  private static final List<DataOutputStream> clientsOutputs = new ArrayList<>();
  private static final List<String> clientNames = new ArrayList<>();
  private static final Map<String, Integer> scores = new HashMap<>();
  public static volatile boolean gameStarted = false;
  public static volatile boolean acceptingAnswers = false;
  public static volatile boolean chatMode = true;
  private static String currentAnswer = null;
  private static final Object answerLock = new Object();
  private static String firstCorrectPlayer = null;

  private static String[][] questions;

  public static final String RED = "\u001B[31m";
  public static final String BLUE = "\u001B[34m";
  public static final String YELLOW = "\u001B[33m";
  public static final String RESET = "\u001B[0m";
  public static final String GREEN = "\u001B[32m";

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

      String name;
      try {
        name = CryptoUtils.decrypt(in.readUTF());
      } catch (Exception e) {
        System.out.println("Error al descifrar nombre del cliente: " + e.getMessage());
        clientSocket.close();
        continue;
      }

      synchronized (clientsOutputs) {
        clientsOutputs.add(out);
        clientNames.add(name);
        scores.put(name, 0);
        System.out.println(name + " se ha conectado. Jugadores: " + clientNames.size());
        broadcast("chat", GREEN + name + " se ha unido a la sala." + RESET);
        broadcast("chat", "Jugadores conectados: " + clientNames.size());
        if (clientNames.size() >= 2 && !gameStarted) {
          broadcast("chat", "Hay " + clientNames.size() + " jugadores. Escriban 'start' para comenzar.");
        }
      }

      ClientHandler handler = new ClientHandler(in, out, name);
      handler.start();
    }
  }

  public static synchronized boolean attemptStart(String name) {
    if (!gameStarted && clientNames.size() >= 2) {
      gameStarted = true;
      chatMode = false;
      broadcast("system", GREEN + "¡Comienza el juego! " + name + " ha iniciado la partida." + RESET);
      new Thread(ServerApp::gameLoop).start();
      return true;
    } else if (clientNames.size() < 2) {
      sendTo(name, "Se necesitan al menos 2 jugadores.");
    }
    return false;
  }

  public static void broadcast(String type, String msg) {
    synchronized (clientsOutputs) {
      for (int i = 0; i < clientsOutputs.size(); i++) {
        try {
          String encrypted = CryptoUtils.encrypt(type + "|" + msg);
          clientsOutputs.get(i).writeUTF(encrypted);
          clientsOutputs.get(i).flush();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static void sendTo(String name, String msg) {
    synchronized (clientsOutputs) {
      for (int i = 0; i < clientNames.size(); i++) {
        if (clientNames.get(i).equals(name)) {
          try {
            String encrypted = CryptoUtils.encrypt("system|" + msg);
            clientsOutputs.get(i).writeUTF(encrypted);
            clientsOutputs.get(i).flush();
          } catch (Exception e) {
            e.printStackTrace();
          }
          break;
        }
      }
    }
  }

  public static void handleChat(String name, String message) {
    if (chatMode) {
      broadcast("chat", name + ": " + message);
    }
  }

  public static void submitAnswer(String name, String answer) {
    synchronized (answerLock) {
      if (acceptingAnswers && currentAnswer != null) {
        if (answer.toLowerCase().equals(currentAnswer.toLowerCase())) {
          if (firstCorrectPlayer == null) {
            firstCorrectPlayer = name;
            int newScore = scores.get(name) + 1;
            scores.put(name, newScore);
          }
        }
      }
    }
  }

  private static void gameLoop() {
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < questions.length; i++)
      indices.add(i);
    Collections.shuffle(indices);
    scores.replaceAll((k, v) -> 0);

    for (int round = 1; round <= MAX_ROUNDS; round++) {
      if (clientNames.size() < 2) {
        broadcast("system", RED + "No hay suficientes jugadores. Juego terminado." + RESET);
        break;
      }

      int idx = indices.get(round - 1);
      String question = questions[idx][0];
      String answer = questions[idx][1];

      chatMode = false;
      firstCorrectPlayer = null;
      broadcast("system", "");
      broadcast("system", BLUE + "══════ RONDA " + round + " ══════" + RESET);
      broadcast("question", RED + question + RESET);
      broadcast("system", "Tienes " + ANSWER_TIME + " segundos para responder.");

      acceptingAnswers = true;
      currentAnswer = answer.toLowerCase();

      for (int sec = ANSWER_TIME; sec > 0; sec--) {
        if (firstCorrectPlayer != null || clientNames.size() < 2)
          break;

        if (sec == 15) {
          broadcast("timer", BLUE + "¡Quedan 15 segundos!" + RESET);
        } else if (sec <= 5) {
          broadcast("timer", BLUE + String.valueOf(sec) + "..." + RESET);
        }

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          break;
        }
      }

      acceptingAnswers = false;

      if (firstCorrectPlayer != null) {
        broadcast("system", GREEN + firstCorrectPlayer + " ha acertado! (+1 punto)" + RESET);
      } else {
        broadcast("system", RED + "¡Se acabó el tiempo! Nadie acertó." + RESET);
        broadcast("system", "La respuesta era: " + answer);
      }

      currentAnswer = null;
      firstCorrectPlayer = null;

      if (round < MAX_ROUNDS) {
        broadcast("system", "");
        broadcast("system", GREEN + "15 segundos para la ronda " + (round + 1) + ". ¡Chat abierto!" + RESET);
        chatMode = true;
        try {
          Thread.sleep(15000);
        } catch (InterruptedException e) {
          break;
        }
      }
    }

    chatMode = false;
    broadcast("system", "");
    broadcast("system", GREEN + "=== JUEGO TERMINADO ===" + RESET);
    broadcast("system", "Clasificación final:");

    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
    sorted.sort((e1, e2) -> e2.getValue() - e1.getValue());

    int pos = 1;
    boolean first = true;
    for (Map.Entry<String, Integer> entry : sorted) {
      if (first) {
        broadcast("system",
            pos + ". " + YELLOW + "🏆 " + entry.getKey() + " - " + entry.getValue() + " puntos 🏆" + RESET);
        first = false;
      } else {
        broadcast("system", pos + ". " + entry.getKey() + " - " + entry.getValue() + " puntos");
      }
      pos++;
    }

    gameStarted = false;
    chatMode = true;
    broadcast("system", "");
    broadcast("system", GREEN + "Chat abierto. Escriban 'start' para jugar otra vez." + RESET);
  }
}