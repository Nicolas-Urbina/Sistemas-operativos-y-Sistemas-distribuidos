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
    private static final Map<Session, String> usuariosActivos = new ConcurrentHashMap<>();
    private static final Map<Session, Long> ultimoPongPorSesion = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService EJECUTOR_HEARTBEAT =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
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
        String nombre = obtenerNombre(sesion);
        usuariosActivos.put(sesion, nombre);
        ultimoPongPorSesion.put(sesion, System.currentTimeMillis());
        difundir("[Sistema]: " + nombre + " se ha unido a la sala.");
    }

    @OnMessage
    public void alRecibirPong(PongMessage pong, Session sesion) {
        ultimoPongPorSesion.put(sesion, System.currentTimeMillis());
    }

    @OnMessage
    public void alRecibirMensaje(String mensaje, Session remitente) {
        String nombre = usuariosActivos.getOrDefault(remitente, "Anónimo");
        difundir("[" + nombre + "]: " + mensaje);
    }

    @OnClose
    public void alCerrar(Session sesion) {
        quitarUsuario(sesion);
    }

    @OnError
    public void alOcurrirError(Session sesion, Throwable error) {
        System.err.println("Error en sesión "
                + (sesion != null ? sesion.getId() : "desconocida") + ": " + error.getMessage());
        if (sesion != null) {
            quitarUsuario(sesion);
        }
    }

    private static void verificarHeartbeats() {
        long ahora = System.currentTimeMillis();
        for (Session sesion : ultimoPongPorSesion.keySet()) {
            if (!sesion.isOpen()) {
                quitarUsuario(sesion);
                continue;
            }

            long ultimoPong = ultimoPongPorSesion.getOrDefault(sesion, ahora);
            if (ahora - ultimoPong >= TIEMPO_MAXIMO_SIN_PONG_MILLIS) {
                try {
                    sesion.close();
                } catch (IOException | IllegalStateException error) {
                    System.err.println("No se pudo cerrar la sesión " + sesion.getId()
                            + ": " + error.getMessage());
                    quitarUsuario(sesion);
                }
                continue;
            }

            try {
                sesion.getBasicRemote().sendPing(ByteBuffer.allocate(0));
            } catch (IOException | IllegalStateException error) {
                System.err.println("No se pudo enviar PING a " + sesion.getId()
                        + ": " + error.getMessage());
                try {
                    sesion.close();
                } catch (IOException | IllegalStateException closeError) {
                    System.err.println("No se pudo cerrar la sesión " + sesion.getId()
                            + ": " + closeError.getMessage());
                }
                quitarUsuario(sesion);
            }
        }
    }

    private static void quitarUsuario(Session sesion) {
        ultimoPongPorSesion.remove(sesion);
        String nombre = usuariosActivos.remove(sesion);
        if (nombre != null) {
            difundir("[Sistema]: " + nombre + " ha salido de la sala.");
        }
    }

    private static void difundir(String texto) {
        for (Session sesion : usuariosActivos.keySet()) {
            if (sesion.isOpen()) {
                try {
                    sesion.getBasicRemote().sendText(texto);
                } catch (IOException error) {
                    System.err.println("No se pudo enviar a " + sesion.getId() + ": " + error.getMessage());
                }
            }
        }
    }

    private String obtenerNombre(Session sesion) {
        return sesion.getRequestParameterMap()
                .getOrDefault("nombre", List.of("Anónimo"))
                .stream()
                .findFirst()
                .orElse("Anónimo")
                .trim();
    }
}
