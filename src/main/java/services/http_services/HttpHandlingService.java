package services.http_services;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpHandlingService {

    public String getResponse(HttpExchange httpExchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream ios = httpExchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }

        return sb.toString().trim();
    }

    public void checkAuth(HttpExchange httpExchange) throws RuntimeException {
        try {
            Headers headers = httpExchange.getRequestHeaders();
            String token = headers.get("Authorization").get(0).replaceAll(".*\\s", "").trim();
            JwtService.validateJwtToken(token);
        } catch (RuntimeException e) {
            throw new RuntimeException("Cannot validate authorization token.");
        }
    }

    public void sendResponse(HttpExchange httpExchange, String responseText, Integer status) throws RuntimeException {
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
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Internal server error");
        }
    }

    public void sendResponse(HttpExchange httpExchange, Integer status) throws IOException {
        httpExchange.sendResponseHeaders(status, -1);
    }

    public void sendErrorResponse(HttpExchange httpExchange) throws RuntimeException, IOException {
        sendErrorResponse(httpExchange, "Error happened...");
    }

    public void sendErrorResponse(HttpExchange httpExchange, @NotNull String errorMessage)  throws RuntimeException {
        sendResponse(httpExchange, errorMessage, 500);
    }

}
