package com.avatarai.server;

import com.avatarai.utils.LocalDateTimeSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

public abstract class ServiceHandler implements HttpHandler
{
    private record Method(Function<ServiceRequest, ServiceResponse> function, ArrayList<String> fieldNames, ArrayList<String> paramNames){}

    HashMap<String, Method> methods = new HashMap<>();
    protected Gson gson =  new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create();

    public final void addMethod(String name, Function<ServiceRequest, ServiceResponse> method, String fieldNames, String paramNames)
    {
        ArrayList<String> fields = new ArrayList<>();
        ArrayList<String> params = new ArrayList<>();
        if (fieldNames != null && !fieldNames.isBlank()) fields = new ArrayList<>(Arrays.asList(fieldNames.trim().split("\\s*,\\s*")));
        if (paramNames != null && !paramNames.isBlank()) params = new ArrayList<>(Arrays.asList(paramNames.trim().split("\\s*,\\s*")));
        methods.put(name, new Method(method, fields, params));
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException
    {
        ServiceResponse response;
        try
        {
            ServiceRequest request = parseRequest(exchange);
            Method method = methods.get(request.function());

            if (method != null)
            {
                checkInputs(request, method);
                response = method.function.apply(request);
            }
            else
            {
                response = new ServiceResponse(404, "Function <" + request.function() + "> not Found");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            response = new ServiceResponse(500, e.getMessage());
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.response().getBytes();
        exchange.sendResponseHeaders(response.statusCode(), bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void checkInputs(ServiceRequest request, Method method) throws IOException
    {
        ArrayList<String> missingFields = new ArrayList<>();
        ArrayList<String> missingParams = new ArrayList<>();

        for (String fieldName : method.fieldNames())
        {
            if (!request.fields().containsKey(fieldName)) missingFields.add(fieldName);
        }

        for (String paramName : method.paramNames())
        {
            if (!request.params().containsKey(paramName)) missingParams.add(paramName);
        }

        if (!missingFields.isEmpty() || !missingParams.isEmpty()) {
            throw new IOException("Error calling service function.\nMissing params: " + missingParams + "\nMissing fields: " + missingFields);
        }
    }

    public static ServiceRequest parseRequest(HttpExchange exchange) throws IOException
    {
        String query = exchange.getRequestURI().getQuery();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, List<String>> fields = parseFields(body);
        Map<String, List<String>> params = parseFields(query);
        Headers headers = exchange.getRequestHeaders();
        List<String> users = headers.get("user-id");
        String user = "test";
        if (users != null && users.get(0) != null)
            user = users.get(0);

        String path = exchange.getRequestURI().getPath();
        String function = path.substring(path.lastIndexOf('/')+1);
        return new ServiceRequest(user, function, fields, params);
    }

    private static Map<String, List<String>> parseFields(String body) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return parameters;

        String[] keyValuePairs = body.split("&");

        for (String keyValuePair : keyValuePairs)
        {
            String[] keyAndValue = keyValuePair.split("=", 2);

            String key = keyAndValue[0];
            String value = keyAndValue.length > 1 ? keyAndValue[1] : "";

            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);

            parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return parameters;
    }

}
