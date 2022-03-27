package server;

import com.sun.net.httpserver.HttpServer;
import services.admin_services.AdminService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    public static HttpServer server;
    private static final int PORT = Integer.parseInt(System.getenv("PORT"));

    public Server() throws IOException {
        server = HttpServer.create(new InetSocketAddress(System.getenv("BASE_URL"), PORT), 0);
    }

    public void start() {
        server.createContext("/", new AdminHttpHandler());
        server.createContext("/auth", new AuthHttpHandler());
        server.start();
        System.out.println("Server is running on port " + PORT + "...");
    }
}
