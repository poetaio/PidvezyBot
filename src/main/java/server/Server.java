package server;

import com.sun.net.httpserver.HttpServer;
import services.admin_services.AdminService;

import java.io.IOException;
import java.net.InetSocketAddress;

import static server.utils.Constants.PORT;

public class Server {

    public static HttpServer server;
    private final AdminService adminService;

    public Server(AdminService adminService) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        this.adminService = adminService;
    }

    public void start() {
        server.createContext("/", new AdminHttpHandler(adminService));
        server.createContext("/auth", new AuthHttpHandler());
        server.start();
        System.out.println("Server is running on port " + PORT + "...");
    }
}
