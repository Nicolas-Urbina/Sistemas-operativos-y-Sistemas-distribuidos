package cl.laboratorio.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/ws/chat")
public class ChatWebSocketServer3 {

    private static final long INTERVALO_HEARTBEAT_SEGUNDOS = 10;
    private static final long TIEMPO_MAXIMO_SIN_PONG_MILLIS = 30_000;
    // Mapa thread-safe para asociar cada Session con el nombre del usuario
    private static final Map<Session, String> usuariosActivos = new ConcurrentHashMap<>();
    private static final Map<Session, Long> ultimoPongPorSesion = new ConcurrentHashMap<>();
    private static final Set<Session> cierresPorHeartbeat = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService EJECUTOR_HEARTBEAT = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread hilo = new Thread(runnable, "websocket-heartbeat");
                hilo.setDaemon(true);
                return hilo;
            });

    static {
        EJECUTOR_HEARTBEAT.scheduleAtFixedRate(
                ChatWebSocketServer3::verificarHeartbeats,
                INTERVALO_HEARTBEAT_SEGUNDOS,
                INTERVALO_HEARTBEAT_SEGUNDOS,
                TimeUnit.SECONDS);
    }

    @OnOpen
    public void alAbrir(Session sesion) {
        // Extrae el nombre enviado en la query string (?nombre=Valor)
        Map<String, List<String>> parametros = sesion.getRequestParameterMap();
        String nombre = "Anónimo";

        if (parametros.containsKey("nombre") && !parametros.get("nombre").isEmpty()) {
            String valor = parametros.get("nombre").get(0).trim();
            if (!valor.isEmpty()) {
                nombre = valor;
            }
        }

        usuariosActivos.put(sesion, nombre);
        ultimoPongPorSesion.put(sesion, System.currentTimeMillis());
        System.out.println("Usuario conectado: " + nombre + " [ID Sesión: " + sesion.getId() + "]");
        difundir("[Sistema]: " + nombre + " se ha unido a la sala.");
        // Opción 2: Notificar la lista actualizada de usuarios activos a todos los
        // clientes
        difundirListaUsuarios();
    }

    @OnMessage
    public void alRecibirPong(PongMessage pong, Session sesion) {
        ultimoPongPorSesion.put(sesion, System.currentTimeMillis());
        System.out.println("[Heartbeat] PONG recibido de " + sesion.getId());
    }

    @OnMessage
    public void alRecibirMensaje(String mensaje, Session remitente) {
        String nombre = usuariosActivos.getOrDefault(remitente, "Anónimo");
        System.out.println("[" + nombre + "]: " + mensaje);
        difundir("[" + nombre + "]: " + mensaje);
    }

    @OnClose
    public void alCerrar(Session sesion, jakarta.websocket.CloseReason razon) {
        ultimoPongPorSesion.remove(sesion);
        String nombre = usuariosActivos.remove(sesion);
        if (nombre != null) {
            if (cierresPorHeartbeat.remove(sesion)) {
                System.out.println("Usuario desconectado por heartbeat (timeout PONG): " + nombre);
            } else {
                System.out.println("Usuario desconectado: " + nombre
                        + " [código: " + razon.getCloseCode().getCode()
                        + ", motivo: " + razon.getReasonPhrase() + "]");
            }
            difundir("[Sistema]: " + nombre + " ha salido de la sala.");
            // Opción 2: Notificar la lista actualizada tras la salida
            difundirListaUsuarios();
        }
    }

    @OnError
    public void alOcurrirError(Session sesion, Throwable error) {
        System.err.println(
                "Error en sesión " + (sesion != null ? sesion.getId() : "desconocida") + ": " + error.getMessage());
        if (sesion != null) {
            ultimoPongPorSesion.remove(sesion);
            String nombre = usuariosActivos.remove(sesion);
            if (nombre != null) {
                difundir("[Sistema]: Se perdió la conexión con " + nombre + ".");
                // Opción 2: Notificar la lista actualizada tras el error
                difundirListaUsuarios();
            }
        }
    }

    private static void verificarHeartbeats() {
        long ahora = System.currentTimeMillis();

        for (Session sesion : ultimoPongPorSesion.keySet()) {
            if (!sesion.isOpen()) {
                ultimoPongPorSesion.remove(sesion);
                continue;
            }

            long ultimoPong = ultimoPongPorSesion.getOrDefault(sesion, ahora);
            if (ahora - ultimoPong >= TIEMPO_MAXIMO_SIN_PONG_MILLIS) {
                cerrarPorHeartbeat(sesion);
                continue;
            }

            try {
                sesion.getBasicRemote().sendPing(ByteBuffer.allocate(0));
                System.out.println("[Heartbeat] PING enviado a " + sesion.getId());
            } catch (IOException | IllegalStateException error) {
                System.err.println("[Heartbeat] No se pudo enviar PING a "
                        + sesion.getId() + ": " + error.getMessage());
                cerrarPorHeartbeat(sesion);
            }
        }
    }

    private static void cerrarPorHeartbeat(Session sesion) {
        cierresPorHeartbeat.add(sesion);
        ultimoPongPorSesion.remove(sesion);
        if (!sesion.isOpen()) {
            return;
        }

        try {
            sesion.close(new jakarta.websocket.CloseReason(
                    jakarta.websocket.CloseReason.CloseCodes.GOING_AWAY,
                    "No se recibió PONG dentro de 30 segundos."));
        } catch (IOException | IllegalStateException error) {
            System.err.println("[Heartbeat] No se pudo cerrar la sesión " + sesion.getId()
                    + ": " + error.getMessage());
        }
    }

    // Opción 2: Envía a todos los clientes conectados el arreglo con los nombres
    // activos
    private void difundirListaUsuarios() {
        // Construimos el mensaje de control con estructura JSON simple usando bucle
        // normal e if/else
        StringBuilder sb = new StringBuilder();
        sb.append("{\"tipo\":\"LISTA_USUARIOS\",\"usuarios\":[");

        boolean primero = true;
        for (String nombre : usuariosActivos.values()) {
            if (primero) {
                sb.append("\"").append(nombre).append("\"");
                primero = false;
            } else {
                sb.append(",\"").append(nombre).append("\"");
            }
        }
        sb.append("]}");

        difundir(sb.toString());
    }

    private void difundir(String texto) {
        for (Session sesion : usuariosActivos.keySet()) {
            if (sesion.isOpen()) {
                try {
                    sesion.getBasicRemote().sendText(texto);
                } catch (IOException e) {
                    System.err.println("Error al emitir a " + sesion.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}