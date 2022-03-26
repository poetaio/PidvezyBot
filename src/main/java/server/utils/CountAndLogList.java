package server.utils;

import models.hibernate.Log;

import java.util.List;

public class CountAndLogList {
    public long total;
    public List<Log> resList;

    public CountAndLogList(long total, List<Log> resList) {
        this.total = total;
        this.resList = resList;
    }
}
