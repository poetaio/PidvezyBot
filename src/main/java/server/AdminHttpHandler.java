package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.utils.LogCriteria;
import models.utils.State;
import server.utils.CountAndLogList;
import server.utils.HttpParamQuery;
import services.admin_services.AdminService;
import services.admin_services.HistoryService;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import static server.utils.Constants.*;

public class AdminHttpHandler implements HttpHandler {

    private final AdminService adminService;
    private final ObjectMapper objectMapper;
    private final HttpHandlingService httpService;
    private final HistoryService historyService;

    public AdminHttpHandler(AdminService adminService) {
        this.adminService = adminService;
        historyService = new HistoryService();
        objectMapper = new ObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm");
        objectMapper.setDateFormat(df);
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
                switch (httpExchange.getRequestURI().getPath()) {
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
                    case HISTORY_RESOURCE:
                        getHistory(httpExchange);
                        break;
                    default:
                        httpService.sendErrorResponse(httpExchange, "No such endpoint...");
                }
            }
            // todo: split into AUTH error and internal server error
        } catch (RuntimeException | JsonProcessingException e) {
//            e.printStackTrace();
            httpService.sendErrorResponse(httpExchange, e.getMessage());
        } catch (Exception e) {
            System.out.println("Internal error occurred");
            httpService.sendErrorResponse(httpExchange, e.getMessage());
        }
    }

    private void getTripQueue(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getTripInQueue()), 200);
    }

    private void getInactiveTrips(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getInactiveTrips()), 200);
    }

    private void getTakenTrips(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getTakenTrips()), 200);
    }

    private void getFinishedTrips(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getFinishedTrips()), 200);
    }

    private void getActiveDrivers(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);
        httpService.sendResponse(httpExchange, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adminService.getActiveDrivers()), 200);
    }

    private void getHistory(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);

        HttpParamQuery paramQuery = new HttpParamQuery(httpExchange.getRequestURI().getQuery());

        Integer limit = paramQuery.getIntParam("limit");
        Integer page = paramQuery.getIntParam("page");

        Timestamp dateFrom = paramQuery.getTimestampParam("dateFrom");
        Timestamp dateTo = paramQuery.getTimestampParam("dateTo");
        State stateFrom = paramQuery.getStateParam("stateFrom");
        State stateTo = paramQuery.getStateParam("stateTo");
        Long userId = paramQuery.getLongParam("userId");

        LogCriteria logCriteria = new LogCriteria(dateFrom, dateTo, stateFrom, stateTo, userId);

        CountAndLogList res = historyService.getAll(page, limit, logCriteria);
        String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res);

        httpService.sendResponse(httpExchange, response, 200);
    }

}