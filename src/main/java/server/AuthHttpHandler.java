package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import services.http_services.HttpHandlingService;
import services.http_services.JwtService;
import models.dao.ResponseTokenDao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static server.utils.Constants.LOGIN_RESOURCE;

public class AuthHttpHandler implements HttpHandler {

    private final HttpHandlingService httpService;

    public AuthHttpHandler() {
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
            if (!"POST".equals(httpExchange.getRequestMethod())) {
                httpService.sendErrorResponse(httpExchange, "Method is not supported");
                return;
            }

            if (!httpExchange.getRequestURI().getPath().equals(LOGIN_RESOURCE)) {
                httpService.sendErrorResponse(httpExchange);
                return;
            }

            String body = httpService.getResponse(httpExchange);
            if (body.isEmpty() || body.isBlank()) {
                httpService.sendErrorResponse(httpExchange, "Empty body");
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> bodyMap = objectMapper.readValue(body, HashMap.class);

            if (!bodyMap.get("password").equals(System.getenv("ADMIN_PASSWORD"))) {
                httpService.sendResponse(httpExchange, "Access denied", 401);
                return;
            }

            ResponseTokenDao tokenObj = new ResponseTokenDao(JwtService.generateToken());
            String response = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(tokenObj);
            httpService.sendResponse(httpExchange, response, 200);
        } catch (Exception e) {
            e.printStackTrace();
            httpService.sendErrorResponse(httpExchange, e.getMessage() == null ? "" : e.getMessage());
        }
    }
}