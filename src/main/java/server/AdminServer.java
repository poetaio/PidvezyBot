package server;

import services.admin_services.AdminService;

import java.io.IOException;

public class AdminServer {

    private AdminService adminService;

    public AdminServer(AdminService adminService) {
        this.adminService = adminService;
    }

    public void startAdminSever() throws IOException {
        Server server = new Server(adminService);
        server.start();
    }

}
