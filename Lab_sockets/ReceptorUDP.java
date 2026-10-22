import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class ReceptorUDP {
    private static final int PUERTO = 5001; // puerto UDP 5001
    private static final int BUFFER_SIZE = 1024; // Tamaño del buffer para recepción

    public static void main(String[] args) {
        System.out.println("Iniciando Receptor de Telemetría UDP en el puerto " + PUERTO + "...");

        // DatagramSocket ligado al puerto 5001 para escuchar datagramas entrantes
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            byte[] buffer = new byte[BUFFER_SIZE];

            System.out.println("Esperando lecturas de sensores (fire-and-forget)...");

            while (!socket.isClosed()) {
                // DatagramPacket para almacenar el paquete entrante
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);

                // receive() bloquea el hilo hasta que arriba un datagrama UDP
                socket.receive(paquete);

                // Desempaquetar los bytes recibidos
                String telemetria = new String(
                        paquete.getData(),
                        paquete.getOffset(),
                        paquete.getLength(),
                        StandardCharsets.UTF_8);

                System.out.println("Telemetría recibida [" + paquete.getAddress().getHostAddress() + ":"
                        + paquete.getPort() + "]: " + telemetria);

            }
        } catch (IOException e) {
            System.err.println("Error en Receptor UDP: " + e.getMessage());
        }
    }
}
