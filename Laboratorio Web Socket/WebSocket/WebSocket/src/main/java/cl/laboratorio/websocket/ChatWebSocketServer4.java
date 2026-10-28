package cl.laboratorio.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/chat-json")
public class ChatWebSocketServer4 {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, Session> sesionesPorUsuario = new ConcurrentHashMap<>();
    private static final Map<Session, String> usuariosPorSesion = new ConcurrentHashMap<>();

    @OnOpen
    public void alAbrir(Session sesion) {
        String nombre = obtenerNombre(sesion);
        if (nombre.isEmpty() || sesionesPorUsuario.putIfAbsent(nombre, sesion) != null) {
            cerrar(sesion, "El nombre está vacío o ya está conectado.");
            return;
        }

        usuariosPorSesion.put(sesion, nombre);
        System.out.println("Usuario conectado: " + nombre);
        difundir(new Trama("PUBLICO", "Sistema", null, nombre + " se ha unido a la sala.", null));
        difundirListaUsuarios();
    }

    @OnMessage
    public void alRecibirMensaje(String contenido, Session remitente) {
        String origen = usuariosPorSesion.get(remitente);
        if (origen == null) {
            return;
        }

        try {
            Trama entrada = JSON.readValue(contenido, Trama.class);
            String mensaje = entrada.mensaje() == null ? "" : entrada.mensaje().trim();
            if (!"PUBLICO".equals(entrada.tipo()) || mensaje.isEmpty()) {
                enviar(remitente, new Trama("ERROR", "Sistema", origen,
                        "La trama debe ser PUBLICO y contener un mensaje.", null));
                return;
            }

            if (mensaje.startsWith("@")) {
                int separador = mensaje.indexOf(' ');
                if (separador <= 1 || separador == mensaje.length() - 1) {
                    enviar(remitente, new Trama("ERROR", "Sistema", origen,
                            "Usa el formato @usuario mensaje.", null));
                    return;
                }
                String destino = mensaje.substring(1, separador).trim();
                String texto = mensaje.substring(separador + 1).trim();
                enviarPrivado(origen, destino, texto);
            } else {
                difundir(new Trama("PUBLICO", origen, null, mensaje, null));
            }
        } catch (IOException error) {
            enviar(remitente, new Trama("ERROR", "Sistema", origen,
                    "La trama JSON no es válida.", null));
            System.err.println("JSON inválido recibido por " + origen + ": " + error.getMessage());
        }
    }

    private void enviarPrivado(String origen, String destino, String mensaje) {
        Session sesionDestino = sesionesPorUsuario.get(destino);
        if (sesionDestino == null) {
            enviar(sesionesPorUsuario.get(origen), new Trama("ERROR", "Sistema", origen,
                    "El usuario @" + destino + " no está conectado.", null));
            return;
        }

        Trama trama = new Trama("PRIVADO", origen, destino, mensaje, null);
        enviar(sesionDestino, trama);
        enviar(sesionesPorUsuario.get(origen), trama);
    }

    @OnClose
    public void alCerrar(Session sesion) {
        quitarUsuario(sesion);
    }

    @OnError
    public void alOcurrirError(Session sesion, Throwable error) {
        System.err.println("Error WebSocket: " + error.getMessage());
        quitarUsuario(sesion);
    }

    private void quitarUsuario(Session sesion) {
        String nombre = usuariosPorSesion.remove(sesion);
        if (nombre != null && sesionesPorUsuario.remove(nombre, sesion)) {
            difundir(new Trama("PUBLICO", "Sistema", null, nombre + " ha salido de la sala.", null));
            difundirListaUsuarios();
        }
    }

    private void difundirListaUsuarios() {
        List<String> usuarios = new ArrayList<>(sesionesPorUsuario.keySet());
        difundir(new Trama("LISTA_USUARIOS", "Sistema", null, null, usuarios));
    }

    private void difundir(Trama trama) {
        for (Session sesion : usuariosPorSesion.keySet()) {
            enviar(sesion, trama);
        }
    }

    private void enviar(Session sesion, Trama trama) {
        if (sesion == null || !sesion.isOpen()) {
            return;
        }
        try {
            sesion.getBasicRemote().sendText(JSON.writeValueAsString(trama));
        } catch (IOException error) {
            System.err.println("No se pudo enviar la trama: " + error.getMessage());
        }
    }

    private String obtenerNombre(Session sesion) {
        String nombre = sesion.getRequestParameterMap().getOrDefault("nombre", List.of(""))
                .stream().findFirst().orElse("").trim();
        return nombre.replaceAll("[^\\p{L}\\p{N}_-]", "");
    }

    private void cerrar(Session sesion, String motivo) {
        enviar(sesion, new Trama("ERROR", "Sistema", null, motivo, null));
        try {
            sesion.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, motivo));
        } catch (IOException error) {
            System.err.println("No se pudo cerrar la sesión: " + error.getMessage());
        }
    }

    public record Trama(String tipo, String origen, String destino, String mensaje, List<String> usuarios) {
    }
}
