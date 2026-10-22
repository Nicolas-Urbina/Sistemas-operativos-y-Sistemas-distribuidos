import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorTCP {
    private static final int PUERTO = 8080;
    private static final int MAX_HILOS = 50;

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_HILOS);

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor a la escucha en el puerto " + PUERTO);

            while (!serverSocket.isClosed()) {
                // accept() bloquea hasta que arriba una nueva conexión TCP
                Socket clienteSocket = serverSocket.accept();
                System.out.println("Conexión entrante desde: " + clienteSocket.getRemoteSocketAddress());
                
                // Despacho del trabajo al pool de hilos
                pool.execute(new ManejadorCliente(clienteSocket));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    private static class ManejadorCliente implements Runnable {
        private final Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // Uso de try-with-resources para garantizar el cierre seguro de streams y socket
            try (
                BufferedReader lector = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                PrintWriter escritor = new PrintWriter(
                    socket.getOutputStream(), true // autoflush habilitado
                )
            ) {
                String lineaEntrada;
                while ((lineaEntrada = lector.readLine()) != null) {
                    String comando = lineaEntrada.trim();
                    if ("QUIT".equalsIgnoreCase(lineaEntrada.trim())) {
                        escritor.println("Conexión finalizada por el cliente.");
                        break;
                    }
                    System.out.println("Recibido [" + socket.getPort() + "]: " + lineaEntrada);
                    
                    // LÓGICA DE AUDITORÍA Y CONTROL
                    if ("STATUS".equalsIgnoreCase(comando)) {   
                         escritor.println("ACK: OK");
                    } else if ("CALIBRATE".equalsIgnoreCase(comando)) {
                        escritor.println("ACK: OK");
                    } else {
                        escritor.println("ERR: INVALID_COMMAND");
                    }
                }
            } catch (IOException e) {
                System.err.println("Excepción I/O con cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    System.out.println("Socket cerrado: " + socket.getRemoteSocketAddress());
                } catch (IOException e) {
                    System.err.println("Fallo al cerrar socket: " + e.getMessage());
                }
            }
        }
    }
}