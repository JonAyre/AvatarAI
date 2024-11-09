package com.avatarai;

import com.avatarai.server.WebServer;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;

public class AvatarEngine
{
    public static void main(String[] args) throws IOException
    {
        HashMap<String, HttpHandler> handlers = new HashMap<>();

        handlers.put("/avatar/", new AvatarHandler());

        WebServer server = new WebServer(args, handlers);
        server.start();
    }
}
