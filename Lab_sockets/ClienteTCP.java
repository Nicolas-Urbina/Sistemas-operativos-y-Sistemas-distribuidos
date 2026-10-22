import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteTCP {
    private static final String HOST = "127.0.0.1";
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        try (
            // connect() implícito al instanciar Socket
            Socket socket = new Socket(HOST, PUERTO);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Conectado exitosamente al servidor " + HOST + ":" + PUERTO);
            System.out.println("Comandos disponibles: STATUS, CALIBRATE, QUIT");

            String lineaTeclado;
            System.out.print("\nIngrese comando: ");
            while ((lineaTeclado = teclado.readLine()) != null) {
                salida.println(lineaTeclado);
                
                String respuesta = entrada.readLine();
                if (respuesta == null) {
                    System.out.println("El servidor cerró la conexión.");
                    break;
                }
                System.out.println("Servidor: " + respuesta);

                if ("QUIT".equalsIgnoreCase(lineaTeclado.trim())) {
                    break;
                }
                System.out.print("\nIngrese comando: ");
            }
        } catch (IOException e) {
            System.err.println("Error de comunicación en cliente: " + e.getMessage());
        }
    }
}