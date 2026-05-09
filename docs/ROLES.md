# Esquema de seguridad basado en roles

Si la aplicación escalara a un proyecto más grande, se implementaría un sistema de control de acceso basado en roles (RBAC). A continuación se define el esquema propuesto.

## Roles definidos

| Rol       | Descripción                          |
| --------- | ------------------------------------ |
| ADMIN     | Control total del sistema            |
| HOST      | Puede gestionar partidas             |
| PLAYER    | Jugador estándar                     |
| SPECTATOR | Solo puede observar, sin interactuar |

## Permisos por rol

| Acción                    | ADMIN | HOST | PLAYER | SPECTATOR |
| ------------------------- | :---: | :--: | :----: | :-------: |
| Iniciar partida           |  ✅   |  ✅  |   ❌   |    ❌     |
| Responder preguntas       |  ✅   |  ✅  |   ✅   |    ❌     |
| Usar el chat              |  ✅   |  ✅  |   ✅   |    ❌     |
| Expulsar jugadores        |  ✅   |  ✅  |   ❌   |    ❌     |
| Añadir/eliminar preguntas |  ✅   |  ❌  |   ❌   |    ❌     |
| Ver clasificación         |  ✅   |  ✅  |   ✅   |    ✅     |
| Gestionar usuarios        |  ✅   |  ❌  |   ❌   |    ❌     |

## Implementación propuesta

Los roles se asignarían al conectarse el usuario y se almacenarían en el servidor. Cada `ClientHandler` tendría un campo `Role role` y antes de ejecutar cualquier acción se verificaría si el rol tiene permiso:

```java
public enum Role {
    ADMIN, HOST, PLAYER, SPECTATOR
}
```

```java
// En ClientHandler, antes de procesar un mensaje:
if (message.equals("start") && !role.canStart()) {
    sendTo(name, "No tienes permisos para iniciar la partida.");
    return;
}
```

Los hosts y administradores se autenticarían con una contraseña al conectarse, almacenada hasheada en el servidor (bcrypt).
