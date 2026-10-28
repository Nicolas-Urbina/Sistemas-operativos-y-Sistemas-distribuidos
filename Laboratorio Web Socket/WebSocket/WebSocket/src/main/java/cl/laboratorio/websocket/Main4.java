package cl.laboratorio.websocket;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main4 {

    private static final String HOST = "0.0.0.0";
    private static final int PUERTO_HTTP = 8082;
    private static final int PUERTO_WS = 8083;

    public static void main(String[] args) {
        HttpServer httpServer = new HttpServer();
        httpServer.addListener(new NetworkListener("grizzly-http-json", HOST, PUERTO_HTTP));

        ServerConfiguration config = httpServer.getServerConfiguration();
        CLStaticHttpHandler staticHandler = new CLStaticHttpHandler(
                Main4.class.getClassLoader(), "static/");
        staticHandler.setFileCacheEnabled(false);
        config.addHttpHandler(staticHandler, "/");

        Server tyrusServer = new Server(HOST, PUERTO_WS, "/", null, ChatWebSocketServer4.class);

        try {
            httpServer.start();
            tyrusServer.start();
            System.out.println("Servicios iniciados (Main4 - protocolo JSON):");
            System.out.println(" - Web HTTP:      http://localhost:" + PUERTO_HTTP + "/index4.html");
            System.out.println(" - WebSocket URI: ws://localhost:" + PUERTO_WS + "/ws/chat-json");
            System.out.println("Presiona ENTER para detener...");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception error) {
            System.err.println("Error al iniciar el servidor JSON: " + error.getMessage());
            error.printStackTrace();
        } finally {
            try {
                tyrusServer.stop();
            } catch (Exception error) {
                System.err.println("No se pudo detener WebSocket: " + error.getMessage());
            }
            httpServer.shutdownNow();
        }
    }
}
