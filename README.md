# 🎯 Trivia Multijugador — Cliente/Servidor con Java Sockets

Juego de preguntas y respuestas en tiempo real para varios jugadores conectados en red.

---

## Requisitos

- Java 11 o superior
- Las preguntas deben estar en `src/main/resources/net/salesianos/resources/questions.json`

---

## Cómo ejecutar

**1. Arranca el servidor:**

```bash
java net.salesianos.server.ServerApp
```

**2. Arranca uno o más clientes** (en terminales separados):

```bash
java net.salesianos.client.ClientApp
```

**3. Introduce tu nombre de usuario** cuando lo pida.

---

## Cómo jugar

1. Conéctate con al menos 2 jugadores.
2. Cualquiera escribe `start` para comenzar la partida.
3. El servidor lanza 5 rondas de preguntas. Tienes 30 segundos por ronda.
4. Escribe tu respuesta directamente y pulsa Enter.
5. El primero en acertar se lleva el punto.
6. Entre rondas hay 15 segundos de chat libre.
7. Al final se muestra la clasificación. Escribe `start` para jugar otra vez.
8. Escribe `exit` para salir.

---

## Configuración

| Parámetro           | Valor por defecto | Dónde cambiarlo                  |
| ------------------- | ----------------- | -------------------------------- |
| Puerto              | `8082`            | `Constants.java`                 |
| Rondas              | `5`               | `ServerApp.java` → `MAX_ROUNDS`  |
| Tiempo por pregunta | `30 seg`          | `ServerApp.java` → `ANSWER_TIME` |

---

## Añadir preguntas

Edita `questions.json` siguiendo este formato:

```json
[
  {
    "pregunta": "¿Tu pregunta aquí?",
    "respuesta": "tu respuesta"
  }
]
```

Las respuestas no distinguen mayúsculas ni minúsculas.
