package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

import static server.utils.Constants.*;

public class CustomHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
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
                    default:
                        sendErrorResponse(httpExchange, "No such endpoint...");
                }
            }
        } catch (RuntimeException e) {
            sendErrorResponse(httpExchange, e.getMessage());
        }
    }

    private void getTripQueue(HttpExchange httpExchange) throws IOException {
        // get trip queue
        String tripQueueResponseJson = "\"tripQueue\": [{\"tripId\":\"ID1\"}, {\"tripId\":\"ID2\"}, {\"tripId\":\"ID3\"}]";
        sendResponse(httpExchange, tripQueueResponseJson, 200);
    }

    private void getInactiveTrips(HttpExchange httpExchange) throws IOException {
        // get inactive trips
        String inactiveTripsResponseJson = "\"inactiveTrips\": [{\"tripId\":\"ID1\"}, {\"tripId\":\"ID2\"}, {\"tripId\":\"ID3\"}]";
        sendResponse(httpExchange, inactiveTripsResponseJson, 200);
    }

    private void getTakenTrips(HttpExchange httpExchange) throws IOException {
        // get taken trips
        String inactiveTripsResponseJson = "\"takenTrips\": [{\"tripId\":\"ID1\"}, {\"tripId\":\"ID2\"}, {\"tripId\":\"ID3\"}]";
        sendResponse(httpExchange, inactiveTripsResponseJson, 200);
    }

    private void getFinishedTrips(HttpExchange httpExchange) throws IOException {
        // get finished trips
        String inactiveTripsResponseJson = "\"finishedTrips\": [{\"tripId\":\"ID1\"}, {\"tripId\":\"ID2\"}, {\"tripId\":\"ID3\"}]";
        sendResponse(httpExchange, inactiveTripsResponseJson, 200);
    }


    private void sendResponse(HttpExchange httpExchange, String responseText, Integer status) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(status, responseText.length());
        outputStream.write(responseText.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private void sendErrorResponse(HttpExchange httpExchange) throws IOException {
        sendErrorResponse(httpExchange, "Error happened...");
    }

    private void sendErrorResponse(HttpExchange httpExchange, String errorMessage) throws IOException {
        sendResponse(httpExchange, errorMessage, 500);
    }

}