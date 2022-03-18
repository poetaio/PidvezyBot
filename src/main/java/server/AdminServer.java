package server;

import java.io.IOException;

public class AdminServer {

    public void startAdminSever() throws IOException {
        Server server = new Server();
        server.start();
    }

}
