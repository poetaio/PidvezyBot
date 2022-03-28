package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.dao.GroupDao;
import models.hibernate.utils.GroupCriteria;
import models.hibernate.utils.GroupStatus;
import models.hibernate.utils.LogCriteria;
import models.utils.State;
import repositories.utils.CountGroupDao;
import repositories.utils.CountLogDao;
import server.utils.HttpParamQuery;
import services.LogService;
import services.admin_services.AdminService;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import static server.utils.Constants.*;

public class AdminHttpHandler implements HttpHandler {

    private final AdminService adminService;
    private final ObjectMapper objectMapper;
    private final HttpHandlingService httpService;
    private final LogService logService;

    public AdminHttpHandler() {
        this.adminService = AdminService.getInstance();
        objectMapper = new ObjectMapper();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm");
        objectMapper.setDateFormat(df);
        httpService = new HttpHandlingService();
        logService = new LogService();
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
            String method = httpExchange.getRequestMethod();
            String path = httpExchange.getRequestURI().getPath();
            if ("GET".equals(method)) {
                switch (path) {
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
                    case ALL_GROUPS_RESOURCE:
                        getAllGroups(httpExchange);
                        break;
                    default:
                        httpService.sendErrorResponse(httpExchange, "No such endpoint...");
                }
                return;
            }
            if ("POST".equals(method)) {
                if (path.equals(ADD_GROUP_RESOURCE)) {
                    addGroupToGroupBot(httpExchange);
                    return;
                }
                httpService.sendErrorResponse(httpExchange, "No such endpoint...");
                return;
            }
            if ("PUT".equals(method)) {
                switch (path) {
                    case SET_GROUP_ACTIVE_RESOURCE:
                        setGroupActive(httpExchange);
                        break;
                    case SET_GROUP_INACTIVE_RESOURCE:
                        setGroupInactive(httpExchange);
                        break;
                    default:
                        httpService.sendErrorResponse(httpExchange, "No such endpoint...");
                }
                return;
            }
            if ("DELETE".equals(method)) {
                if (path.equals(REMOVE_GROUP_RESOURCE)) {
                    removeGroupFromGroupBot(httpExchange);
                    return;
                }
                httpService.sendErrorResponse(httpExchange, "No such endpoint...");
            }
            // todo: split into AUTH error and internal server error
        } catch (RuntimeException | JsonProcessingException e) {
//            e.printStackTrace();
            httpService.sendErrorResponse(httpExchange, e.getMessage());
        } catch (Exception e) {
            System.out.println("Internal error occurred");
            httpService.sendErrorResponse(httpExchange);
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

        CountLogDao res = logService.getAll(page, limit, logCriteria);
        String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res);

        httpService.sendResponse(httpExchange, response, 200);
    }

    private void getAllGroups(HttpExchange httpExchange) throws RuntimeException, JsonProcessingException {
        httpService.checkAuth(httpExchange);

        HttpParamQuery query = new HttpParamQuery(httpExchange.getRequestURI().getQuery());

        Integer limit = query.getIntParam("limit");
        Integer page = query.getIntParam("page");
        Long groupId = query.getLongParam("groupId");
        String groupName = query.getStringParam("groupName");
        GroupStatus groupStatus = query.getGroupStatus("groupStatus");

        CountGroupDao countGroupDao = adminService.getGroups(page, limit, new GroupCriteria(groupId, groupName, groupStatus));

        String response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(countGroupDao);

        httpService.sendResponse(httpExchange, response, 200);
    }

    private void setGroupActive(HttpExchange httpExchange) throws RuntimeException, IOException {
        httpService.checkAuth(httpExchange);

        HttpParamQuery query = new HttpParamQuery(httpExchange.getRequestURI().getQuery());

        Long groupId = query.getLongParam("groupId");
        if (groupId == null) {
            httpService.sendResponse(httpExchange, "No group id provided", 400);
            return;
        }

        AdminService.getInstance().activateGroupInGroupBot(groupId);

        httpService.sendResponse(httpExchange, 200);
    }

    private void setGroupInactive(HttpExchange httpExchange) throws RuntimeException, IOException {
        httpService.checkAuth(httpExchange);

        HttpParamQuery query = new HttpParamQuery(httpExchange.getRequestURI().getQuery());

        Long groupId = query.getLongParam("groupId");
        if (groupId == null) {
            httpService.sendResponse(httpExchange, "No group id provided", 400);
            return;
        }

        AdminService.getInstance().deactivateGroupFromGroupBot(groupId);

        httpService.sendResponse(httpExchange, 200);
    }

    private void addGroupToGroupBot(HttpExchange httpExchange) throws RuntimeException, IOException {
        httpService.checkAuth(httpExchange);

        GroupDao newGroup = new ObjectMapper().readValue(httpExchange.getRequestBody(), GroupDao.class);
        if (newGroup.getGroupId() == null) {
            httpService.sendResponse(httpExchange, "No group id provided", 400);
            return;
        }

        if (newGroup.getGroupName() == null) {
            httpService.sendResponse(httpExchange, "No group name provided", 400);
            return;
        }

        AdminService.getInstance().addGroupToGroupBot(newGroup.getGroupId(), newGroup.getGroupName());

        httpService.sendResponse(httpExchange, 200);
    }

    private void removeGroupFromGroupBot(HttpExchange httpExchange) throws RuntimeException, IOException {
        httpService.checkAuth(httpExchange);

        HttpParamQuery query = new HttpParamQuery(httpExchange.getRequestURI().getQuery());

        Long groupId = query.getLongParam("groupId");
        if (groupId == null) {
            httpService.sendResponse(httpExchange, "No group id provided", 400);
            return;
        }

        AdminService.getInstance().removeGroupFromGroupBot(groupId);

        httpService.sendResponse(httpExchange, 200);
    }

}