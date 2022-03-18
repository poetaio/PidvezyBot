package server;

import com.sun.net.httpserver.HttpServer;
import services.admin_services.AdminService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    public static HttpServer server;
    private static final int PORT = Integer.parseInt(System.getenv("PORT"));
    private AdminService adminService;

    public Server(AdminService adminService) throws IOException {
        server = HttpServer.create(new InetSocketAddress(System.getenv("BASE_URL"), PORT), 0);
        this.adminService = adminService;
    }

    public void start() {
        server.createContext("/", new CustomHttpHandler(adminService));
        server.start();
        System.out.println("Server is running on port " + PORT + "...");
    }
}
