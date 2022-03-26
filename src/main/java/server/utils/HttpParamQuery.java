package server.utils;

import models.utils.State;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HttpParamQuery {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private Map<String, String> queryParamMap;

    public HttpParamQuery(String query) {
        queryParamMap = queryToMap(query);
    }

    private Map<String, String> queryToMap(String query) {
        if (query == null) return null;

        Map<String, String> resMap = new HashMap<>();
        for (String queryParamStr : query.split("&")) {
            String[] paramKeyValue = queryParamStr.split("=");
            if (paramKeyValue.length < 1) continue;
            if (paramKeyValue.length > 2) resMap.put(paramKeyValue[0], "");
            resMap.put(paramKeyValue[0], paramKeyValue[1]);
        }

        return resMap;
    }

    public Integer getIntParam(String paramName) {
        if (queryParamMap == null) return null;

        try {
            String queryParamStr = queryParamMap.get(paramName);
            return queryParamStr == null ? null : Integer.parseInt(queryParamStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Long getLongParam(String paramName) {
        if (queryParamMap == null) return null;

        try {
            String stateStr = queryParamMap.get(paramName);
            return stateStr == null ? null : Long.parseLong(stateStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Timestamp getTimestampParam(String paramName) {
        if (queryParamMap == null) return null;

        String dateStr = queryParamMap.get(paramName);
        if (dateStr == null) return null;

        try {
            LocalDateTime dateTime = LocalDateTime.from(DATE_TIME_FORMATTER.parse(dateStr));
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeException e) {
            return null;
        }
    }

    public State getStateParam(String paramName) {
        if (paramName == null) return null;

        try {
            String stateStr = queryParamMap.get(paramName);
            return stateStr == null ? null : State.valueOf(stateStr);
        } catch(IllegalArgumentException e) {
            return null;
        }
    }
}
