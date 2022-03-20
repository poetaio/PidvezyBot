package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import services.admin_services.AdminService;

import java.io.IOException;

import static server.utils.Constants.*;

public class AdminHttpHandler implements HttpHandler {

    private final AdminService adminService;
    private final ObjectMapper objectMapper;
    private final HttpHandlingService httpService;

    public AdminHttpHandler(AdminService adminService) {
        this.adminService = adminService;
        objectMapper = new ObjectMapper();
        httpService = new HttpHandlingService();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if (httpExchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
                httpExchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equals(httpExchange.getRequestMethod())) {
                switch (httpExchange.getRequestURI().toString()) {
                    case TRIP_QUEUE_RESOURCE:
                        getTripQueue(httpExchange);
                        break;
                    case INACTIVE_TRIPS_RESOURCE:
                        getInactiveTrips(httpExchange);
                        break;
                    case TAKEN_TRIPS_RESOURCE:
                        getTakenTrips(httpExchange);
                        break;
                    case FINISHED_TRIPS_RESOURCE:
                        getFinishedTrips(httpExchange);
                        break;
                    case ACTIVE_DRIVERS_RESOURCE:
                        getActiveDrivers(httpExchange);
                        break;
                    default:
                        httpService.sendErrorResponse(httpExchange, "No such endpoint...");
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            httpService.sendErrorResponse(httpExchange, e.getMessage());
        }
    }

    private void getTripQueue(HttpExchange httpExchange) throws IOException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getTripInQueue()), 200);
    }

    private void getInactiveTrips(HttpExchange httpExchange) throws IOException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getInactiveTrips()), 200);
    }

    private void getTakenTrips(HttpExchange httpExchange) throws IOException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getTakenTrips()), 200);
    }

    private void getFinishedTrips(HttpExchange httpExchange) throws IOException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getFinishedTrips()), 200);
    }

    private void getActiveDrivers(HttpExchange httpExchange) throws IOException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getActiveDrivers()), 200);
    }

}