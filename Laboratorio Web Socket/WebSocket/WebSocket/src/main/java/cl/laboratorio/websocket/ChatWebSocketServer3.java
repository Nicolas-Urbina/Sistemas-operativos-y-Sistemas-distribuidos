package cl.laboratorio.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/chat")
public class ChatWebSocketServer3 {

    // Mapa thread-safe para asociar cada Session con el nombre del usuario
    private static final Map<Session, String> usuariosActivos = new ConcurrentHashMap<>();

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
        System.out.println("Usuario conectado: " + nombre + " [ID Sesión: " + sesion.getId() + "]");
        difundir("[Sistema]: " + nombre + " se ha unido a la sala.");
        // Opción 2: Notificar la lista actualizada de usuarios activos a todos los
        // clientes
        difundirListaUsuarios();
    }

    @OnMessage
    public void alRecibirMensaje(String mensaje, Session remitente) {
        String nombre = usuariosActivos.getOrDefault(remitente, "Anónimo");
        System.out.println("[" + nombre + "]: " + mensaje);
        difundir("[" + nombre + "]: " + mensaje);
    }

    @OnClose
    public void alCerrar(Session sesion) {
        String nombre = usuariosActivos.remove(sesion);
        if (nombre != null) {
            System.out.println("Usuario desconectado: " + nombre);
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
            String nombre = usuariosActivos.remove(sesion);
            if (nombre != null) {
                difundir("[Sistema]: Se perdió la conexión con " + nombre + ".");
                // Opción 2: Notificar la lista actualizada tras el error
                difundirListaUsuarios();
            }
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