package com.avatarai.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

public class HTMLHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        URI path = URI.create(exchange.getHttpContext().getPath());
        String filename = path.relativize(uri).getPath();
        OutputStream os = exchange.getResponseBody();
        if (filename.isEmpty())
        {
            String response = "<!DOCTYPE html><html lang=\"en\"><head><meta http-equiv=\"refresh\" content=\"0; url=index.html\" /></head><body></body></html>";
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
            os.close();
        }
        else
        {
            File file = new File("web/" + filename);
            if (file.exists() && file.isFile()) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, fileContent.length);
                os.write(fileContent);
                os.close();
            } else {
                String msg = "File not found";
                exchange.sendResponseHeaders(404, msg.length());
                os.write(msg.getBytes());
            }
        }
        os.close();
    }
}