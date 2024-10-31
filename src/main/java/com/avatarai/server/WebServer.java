package com.avatarai.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer
{
    HttpServer server;

    public WebServer(String[] args, HashMap<String, HttpHandler> routes) throws IOException
    {
        this(parseArgsToAddress(args), routes);
    }

    public WebServer(InetSocketAddress address, HashMap<String, HttpHandler> routes) throws IOException
    {
        // Create the server to listen on the specified socket address
        server = HttpServer.create(address, 0);

        // Create a context for each of the specified routes (plus the standard default routes)
        for (Map.Entry<String, HttpHandler> entry : routes.entrySet())
        {
            addRoute(entry.getKey(), entry.getValue());
        }
        addRoute("/test/", new TestHandler());
        addRoute("/", new HTMLHandler());
    }

    public static InetSocketAddress parseArgsToAddress(String[] args)
    {
        Options options = new Options();

        Option ipOption = new Option("ip", "ipaddress", true, "ip address of this server");
        ipOption.setRequired(false);
        options.addOption(ipOption);

        Option portOption = new Option("p", "port", true, "port on which the server will listen");
        portOption.setRequired(false);
        options.addOption(portOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;//not a good practice, it serves it purpose

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String ip = cmd.getOptionValue("ipaddress");
        String portStr = cmd.getOptionValue("port");
        int port;

        if (ip == null) ip = "0.0.0.0";
        if (portStr == null)
            port = 8080;
        else
            port = Integer.parseInt(portStr);

        return new InetSocketAddress(ip, port);
    }

    // Start the server
    public void start()
    {
        server.setExecutor(Executors.newCachedThreadPool()); // Use the default executor
        server.start();
        System.out.println("Server is running on " + server.getAddress().toString());
    }

    public void stop()
    {
        server.stop(0);
    }

    public void addRoute(String path, HttpHandler handler)
    {
        server.createContext(path, handler);
    }
}
