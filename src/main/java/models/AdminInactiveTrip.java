package models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.dao.UserDao;

import java.util.LinkedList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class AdminInactiveTrip {
    private UserDao passenger;
    private String address;
    private String details;
}
