package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import server.utils.JwtUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpHandlingService {

    protected String getResponse(HttpExchange httpExchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream ios = httpExchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }

        return sb.toString().trim();
    }

    protected void checkAuth(HttpExchange httpExchange) {
        try {
            Headers headers = httpExchange.getRequestHeaders();
            String token = headers.get("Authorization").get(0).replaceAll(".*\\s", "").trim();
            JwtUtils.validateJwtToken(token);
        } catch (RuntimeException e) {
            System.out.println("Error during auth check");
            throw new RuntimeException(e.getMessage());
        }
    }

    protected void sendResponse(HttpExchange httpExchange, String responseText, Integer status) {
        try {
            byte[] bs = responseText.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(status, bs.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(bs);
            os.flush();
            os.close();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't send response. " + e.getMessage());
        }
    }

    protected void sendErrorResponse(HttpExchange httpExchange) throws IOException {
        sendErrorResponse(httpExchange, "Error happened...");
    }

    protected void sendErrorResponse(HttpExchange httpExchange, String errorMessage) throws IOException {
        sendResponse(httpExchange, errorMessage, 500);
    }

}
