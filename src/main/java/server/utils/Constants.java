package server.utils;

public interface Constants {

    String TRIP_QUEUE_RESOURCE = "/tripQueue";
    String INACTIVE_TRIPS_RESOURCE = "/inactiveTrips";
    String TAKEN_TRIPS_RESOURCE = "/takenTrips";
    String FINISHED_TRIPS_RESOURCE = "/finishedTrips";
    String ACTIVE_DRIVERS_RESOURCE = "/activeDrivers";
    String HISTORY_RESOURCE =  "/history";

    String ALL_GROUPS_RESOURCE = "/group";
    String SET_GROUP_ACTIVE_RESOURCE = "/group/unban";
    String SET_GROUP_INACTIVE_RESOURCE = "/group/ban";
    String ADD_GROUP_RESOURCE = "/group/add";
    String REMOVE_GROUP_RESOURCE = "/group/remove";

    String LOGIN_RESOURCE = "/auth/login";

    int DEFAULT_LIMIT = 10;
}
