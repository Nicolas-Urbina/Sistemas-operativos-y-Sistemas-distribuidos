package cl.laboratorio.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/chat")
public class ChatWebSocketServer2 {

    private static final Map<Session, String> usuariosActivos = new ConcurrentHashMap<>();

    @OnOpen
    public void alAbrir(Session sesion) {
        String nombre = obtenerNombre(sesion);
        usuariosActivos.put(sesion, nombre);
        difundir("[Sistema]: " + nombre + " se ha unido a la sala.");
        difundirListaUsuarios();
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

    private void quitarUsuario(Session sesion) {
        String nombre = usuariosActivos.remove(sesion);
        if (nombre != null) {
            difundir("[Sistema]: " + nombre + " ha salido de la sala.");
            difundirListaUsuarios();
        }
    }

    private void difundirListaUsuarios() {
        StringBuilder json = new StringBuilder("{\"tipo\":\"LISTA_USUARIOS\",\"usuarios\":[");
        boolean primero = true;
        for (String nombre : usuariosActivos.values()) {
            if (!primero) {
                json.append(',');
            }
            json.append('"').append(escaparJson(nombre)).append('"');
            primero = false;
        }
        json.append("]}");
        difundir(json.toString());
    }

    private void difundir(String texto) {
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

    private String escaparJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
