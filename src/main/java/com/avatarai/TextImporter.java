package com.avatarai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class TextImporter {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/embed";
    private static final String OLLAMA_EMBED_MODEL = "nomic-embed-text";

    public static double[] getEmbeddings(String input) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Create the request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("input", input);
        HttpRequest request;

        // Create the request for ollama API
        requestBody.addProperty("model", OLLAMA_EMBED_MODEL);
        request = HttpRequest.newBuilder()
                .uri(new URI(OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(requestBody)))
                .build();

        // Send the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse the response
            JsonObject responseJson = new Gson().fromJson(response.body(), JsonObject.class);
            return new Gson().fromJson(responseJson.getAsJsonArray("embeddings").get(0).getAsJsonArray(),
                    double[].class);
        } else {
            throw new RuntimeException("Failed to get embeddings: " + response.body());
        }
    }
}