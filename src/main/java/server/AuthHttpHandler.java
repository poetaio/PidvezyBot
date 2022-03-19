package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.utils.JwtUtils;

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
            if ("POST".equals(httpExchange.getRequestMethod())) {
                if (httpExchange.getRequestURI().toString().equals(LOGIN_RESOURCE)) {
                    String body = httpService.getResponse(httpExchange);
                    Map<String, Object> bodyMap = new ObjectMapper().readValue(body, HashMap.class);
                    if (bodyMap.get("password").equals(System.getenv("ADMIN_PASSWORD"))) {
                        httpService.sendResponse(httpExchange, JwtUtils.generateToken(), 200);
                        return;
                    }
                    httpService.sendResponse(httpExchange, "Access denied", 200);
                    return;
                }
                httpService.sendErrorResponse(httpExchange);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            httpService.sendErrorResponse(httpExchange, e.getMessage());
        }
    }
}