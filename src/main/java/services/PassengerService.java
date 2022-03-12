package services;

import org.telegram.telegrambots.meta.api.objects.User;

public class PassengerService {

    /**
     * Saving user info to database
     * @param passengerChatId User-Bot chat id
     * @param passengerInfo User entity, contains user id, username, first name, last name
     */
    public void addPassengerInfo(long passengerChatId, User passengerInfo) {

    }

    /**
     * Creating an active trip by saving passenger address
     * @param passengerUserId User account id
     * @param address Destination address
     */
    public void addAddress(long passengerUserId, String address) {

    }

    /**
     * Adding details to an active trip by userId
     * @param passengerUserId User account id
     * @param details Details of the trip
     */
    public void addDetails(long passengerUserId, String details) {

    }
}
