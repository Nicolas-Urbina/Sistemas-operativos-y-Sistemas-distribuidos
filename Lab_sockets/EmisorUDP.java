import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

public class EmisorUDP {
    private static final String HOST_DESTINO = "127.0.0.1";
    private static final int PUERTO_DESTINO = 5001; // puerto UDP 5001
    private static final int INTERVALO_MS = 500; // Transmisión cada 500 ms

    public static void main(String[] args) {
        System.out.println("Iniciando Sensor Emisor UDP hacia " + HOST_DESTINO + ":" + PUERTO_DESTINO);

        Random random = new Random();
        int sensorId = 1;

        // DatagramSocket sin puerto específico (el SO le asigna un puerto efímero de
        // salida)
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress direccionDestino = InetAddress.getByName(HOST_DESTINO);
            int contadorEnvios = 1;

            while (true) {
                // Simulación de valores de temperatura y humedad
                double temp = 20.0 + (random.nextDouble() * 10.0); // Entre 20.0 y 30.0 °C
                double humedad = 40.0 + (random.nextDouble() * 30.0); // Entre 40% y 70%
                long timestamp = System.currentTimeMillis() / 1000L; // Timestamp en segundos

                // Construcción del mensaje en formato JSON
                String jsonTelemetria = String.format(
                        Locale.US,
                        "{\"sensorId\": %d, \"temp\": %.2f, \"humedad\": %.2f, \"timestamp\": %d}",
                        sensorId, temp, humedad, timestamp);

                // Armado del buffer de bytes (byte[])
                byte[] buffer = jsonTelemetria.getBytes(StandardCharsets.UTF_8);

                // Creación del DatagramPacket con el buffer, longitud, IP y puerto destino
                DatagramPacket paquete = new DatagramPacket(
                        buffer,
                        buffer.length,
                        direccionDestino,
                        PUERTO_DESTINO);

                // Envío del paquete
                socket.send(paquete);

                System.out.println("Enviado (#" + contadorEnvios++ + "): " + jsonTelemetria);

                Thread.sleep(INTERVALO_MS);
            }
        } catch (IOException e) {
            System.err.println("Error de red en Emisor UDP: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Sensor interrumpido: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
