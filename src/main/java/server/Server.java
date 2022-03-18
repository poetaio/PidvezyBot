package server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import static server.utils.Constants.PORT;

public class Server {

    public static HttpServer server;

    public Server() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
    }

    public void start() {
        server.createContext("/", new CustomHttpHandler());
        server.start();
        System.out.println("Server is started on port " + PORT + "...");
    }
}
