## Comparación

| Opción | Implementación | Estructura lógica | Comunicación | HTTP | WebSocket |
|---|---|---|---|---:|---:|
| 1. Mensajería privada y protocolo JSON | `Main4` + `ChatWebSocketServer4` | Mantiene dos mapas concurrentes: usuario → sesión y sesión → usuario. Valida nombres únicos, deserializa tramas y decide entre difusión pública, envío privado o error. | Todas las aplicaciones envían y reciben tramas JSON con `tipo`, `origen`, `destino`, `mensaje` y, para presencia, `usuarios`. | 8082 | 8083 (`/ws/chat-json`) |
| 2. Presencia activa | `Main2` + `ChatWebSocketServer2` | Mantiene las sesiones y sus nombres en un mapa concurrente. Al abrir, cerrar o detectar un error actualiza el conjunto y vuelve a publicar la lista completa. | Los mensajes de chat siguen siendo texto; la presencia se comunica mediante una trama de control JSON `LISTA_USUARIOS`. | 8080 | 8081 (`/ws/chat`) |
| 3. Heartbeat / ping-pong | `Main3` + `ChatWebSocketServer3` | Mantiene sesiones, nombres y la hora del último `PONG`. Un planificador envía `PING` cada 10 segundos y cierra sesiones sin respuesta durante 30 segundos. | El chat usa texto. `PING` y `PONG` son tramas de control WebSocket y no se muestran como mensajes del chat. | 8084 | 8085 (`/ws/chat`) |

### Diferencias principales

- **Capa de aplicación:** la Opción 1 define un protocolo de negocio explícito
  sobre WebSocket; las Opciones 2 y 3 usan texto para el chat y reservan JSON o
  tramas de control para una capacidad concreta.
- **Estado distribuido:** la Opción 2 difunde el estado completo de presencia
  cada vez que cambia. La Opción 3 no difunde una lista: verifica la salud de
  cada canal individualmente.
- **Direccionamiento:** en la Opción 1 el servidor localiza al destinatario
  mediante el mapa de nombres y `@usuario mensaje` genera un mensaje unicast.
  Las otras dos opciones solo difunden el texto a la sala.
- **Resiliencia:** la Opción 2 depende de los eventos de ciclo de vida
  (`@OnOpen`, `@OnClose` y `@OnError`); la Opción 3 agrega detección activa de
  conexiones half-open mediante timeout.
- **Independencia:** aunque las Opciones 2 y 3 usan la misma ruta lógica
  `/ws/chat`, escuchan en puertos distintos y se cargan en procesos distintos.
  La Opción 1 usa además `/ws/chat-json`, evitando ambigüedad de protocolo.

## Verificación de requisitos

- **Opción 1:** implementada en `ChatWebSocketServer4`. El payload contiene los
  campos solicitados; los mensajes públicos se difunden y `@usuario mensaje`
  se entrega únicamente al destinatario y al emisor. Los nombres vacíos,
  duplicados y usuarios inexistentes generan una trama `ERROR`.
- **Opción 2:** implementada de forma independiente en
  `ChatWebSocketServer2`. Cada alta, baja o error publica
  `LISTA_USUARIOS`; `index2.html` reconstruye el elemento `<ul>` lateral.
- **Opción 3:** implementada de forma independiente en
  `ChatWebSocketServer3`. El servidor envía `PING` cada 10 segundos, registra
  `PONG`, y cierra y libera los mapas de una sesión que supera los 30 segundos
  sin respuesta.
- **Separación comprobada:** no hay puertos repetidos entre las tres opciones:
  `8080/8081`, `8082/8083` y `8084/8085`. La Opción 2 ya no comparte la lógica
  de heartbeat y la Opción 3 ya no publica la lista de presencia.

## Requisitos

- JDK 17 o superior.
- Maven 3.8 o superior.

## Ejecución

Desde esta carpeta (`Laboratorio Web Socket/WebSocket`):

```bash
mvn clean compile
```

Ejecutar una opción en una terminal:

```bash
# Opción 1: protocolo JSON y mensajería privada
mvn exec:java -Dexec.mainClass=cl.laboratorio.websocket.Main4
# Abrir http://localhost:8082/index4.html

# Opción 2: presencia activa
mvn exec:java -Dexec.mainClass=cl.laboratorio.websocket.Main2
# Abrir http://localhost:8080/index2.html

# Opción 3: heartbeat
mvn exec:java -Dexec.mainClass=cl.laboratorio.websocket.Main3
# Abrir http://localhost:8084/index3.html
```

Para detener el servidor, presiona `ENTER` en la terminal donde se está
ejecutando. Para comprobar la presencia o los mensajes privados, abre dos o
más ventanas del navegador con nombres distintos. En la Opción 1, escribe
`@pedro hola` para enviar un mensaje privado a `pedro`.

El `pom.xml` conserva `Main3` como clase principal predeterminada, por lo que
`mvn exec:java` ejecuta la Opción 3 si no se proporciona
`-Dexec.mainClass`.
