package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import services.admin_services.AdminService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static server.utils.Constants.*;

public class CustomHttpHandler implements HttpHandler {

    private AdminService adminService;
    private ObjectMapper objectMapper;

    public CustomHttpHandler(AdminService adminService) {
        this.adminService = adminService;
        objectMapper = new ObjectMapper();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        System.out.println("Handling...");
        try {
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
                    case ACTIVE_DRIVERS:
                        getActiveDrivers(httpExchange);
                        break;
                    default:
                        sendErrorResponse(httpExchange, "No such endpoint...");
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendErrorResponse(httpExchange, e.getMessage());
        }
    }

    private void getTripQueue(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getTripInQueue()), 200);
    }

    private void getInactiveTrips(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getInactiveTrips()), 200);
    }

    private void getTakenTrips(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getTakenTrips()), 200);
    }

    private void getFinishedTrips(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getFinishedTrips()), 200);
    }

    private void getActiveDrivers(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getActiveDrivers()), 200);
    }


    private void sendResponse(HttpExchange httpExchange, String responseText, Integer status) {
        try {
            byte[] bs = responseText.getBytes(StandardCharsets.UTF_16);
            httpExchange.sendResponseHeaders(200, bs.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(bs);
            os.flush();
            os.close();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't send response. " + e.getMessage());
        }
    }

    private void sendErrorResponse(HttpExchange httpExchange) throws IOException {
        sendErrorResponse(httpExchange, "Error happened...");
    }

    private void sendErrorResponse(HttpExchange httpExchange, String errorMessage) throws IOException {
        sendResponse(httpExchange, errorMessage, 500);
    }

}