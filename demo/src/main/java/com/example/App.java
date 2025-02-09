package com.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class App {
    public static void main(String[] args) throws Exception {
        int puerto = 6789;
        ServerSocket serverSocket = new ServerSocket(puerto);

        System.out.println("Servidor iniciado en el puerto " + puerto);
        System.out.println("Directorio de trabajo actual: " + new File(".").getCanonicalPath());

        while (true) {
            Socket connectionSocket = serverSocket.accept();
            System.out.println("Nueva conexión aceptada...");
            
            SolicitudHttp solicitud = new SolicitudHttp(connectionSocket);
            Thread hilo = new Thread(solicitud);
            hilo.start();
        }
    }
}

class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    private Socket socket;

    public SolicitudHttp(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            procesarSolicitud();
        } catch (Exception e) {
            System.out.println("Error procesando la solicitud: " + e.getMessage());
        }
    }

    private void procesarSolicitud() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        String lineaDeSolicitud = in.readLine();
        if (lineaDeSolicitud == null) {
            socket.close();
            return;
        }

        System.out.println("Solicitud recibida: " + lineaDeSolicitud);

        StringTokenizer tokenizer = new StringTokenizer(lineaDeSolicitud);
        String method = tokenizer.nextToken();
        String resource = tokenizer.nextToken();

        if (!method.equals("GET")) {
            enviarRespuesta405(out);
            return;
        }

        
        //resource = resource.replace("..", "").replaceAll("//", "/");

        // Define the correct base directory for resources
        File baseDirectory = new File("demo/src/resources").getCanonicalFile();
        File requestedFile = new File(baseDirectory, resource).getCanonicalFile();

        System.out.println("Buscando archivo en: " + requestedFile.getAbsolutePath());

        // Ensure requested file is inside the base directory (security check)
        if (!requestedFile.getAbsolutePath().startsWith(baseDirectory.getAbsolutePath()) || !requestedFile.exists()) {
            enviarRespuesta404(out);
        } else {
            enviarArchivo(requestedFile, out);
        }

        out.close();
        in.close();
        socket.close();
    }

    private void enviarRespuesta404(BufferedOutputStream out) throws IOException {
        String response = "HTTP/1.0 404 Not Found" + CRLF +
                "Content-Type: text/html" + CRLF +
                "Content-Length: 50" + CRLF +
                CRLF +
                "<html><body><h1>404 Not Found</h1></body></html>";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void enviarRespuesta405(BufferedOutputStream out) throws IOException {
        String response = "HTTP/1.0 405 Method Not Allowed" + CRLF +
                "Content-Type: text/html" + CRLF +
                "Content-Length: 55" + CRLF +
                CRLF +
                "<html><body><h1>405 Method Not Allowed</h1></body></html>";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void enviarArchivo(File file, BufferedOutputStream out) throws IOException {
        String contentType = contentType(file.getName());
        long filesize = file.length();

        String header = "HTTP/1.0 200 OK" + CRLF +
                        "Content-Type: " + contentType + CRLF +
                        "Content-Length: " + filesize + CRLF +
                        "Connection: close" + CRLF +
                        CRLF;
        
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.flush();
        
        try (InputStream fis = new FileInputStream(file)) {
            enviarBytes(fis, out);
        }
    }

    private static void enviarBytes(InputStream fis, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int bytes;
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String nombreArchivo) {
        if (nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) return "text/html";
        if (nombreArchivo.endsWith(".jpg") || nombreArchivo.endsWith(".jpeg")) return "image/jpeg";
        if (nombreArchivo.endsWith(".gif")) return "image/gif";
        if (nombreArchivo.endsWith(".png")) return "image/png";
        if (nombreArchivo.endsWith(".css")) return "text/css";
        if (nombreArchivo.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }
}
