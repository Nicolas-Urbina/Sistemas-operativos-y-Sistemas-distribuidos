## Requisitos

- JDK 17 o superior.
- Maven 3.8 o superior.

## Ejecución

Desde esta carpeta (`Laboratorio Web Socket/WebSocket`):

```bash
mvn clean compile
```

Ejecutar una opción en una terminal:

```bash
# Opción 1: protocolo JSON y mensajería privada
mvn exec:java -Dexec.mainClass=cl.laboratorio.websocket.Main4
# Abrir http://localhost:8082/index4.html

# Opción 2: presencia activa
mvn exec:java -Dexec.mainClass=cl.laboratorio.websocket.Main2
# Abrir http://localhost:8080/index2.html

# Opción 3: heartbeat
mvn exec:java -Dexec.mainClass=cl.laboratorio.websocket.Main3
# Abrir http://localhost:8084/index3.html
```

Para detener el servidor, presiona `ENTER` en la terminal donde se está
ejecutando. Para comprobar la presencia o los mensajes privados, abre dos o
más ventanas del navegador con nombres distintos. En la Opción 1, escribe
`@pedro hola` para enviar un mensaje privado a `pedro`.

El `pom.xml` conserva `Main3` como clase principal predeterminada, por lo que
`mvn exec:java` ejecuta la Opción 3 si no se proporciona
`-Dexec.mainClass`.
