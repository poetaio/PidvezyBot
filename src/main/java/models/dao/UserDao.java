package models.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import models.utils.State;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDao {
    private long chatId;
    private State currentState;
    private long userId;
    private String firstName;
    private String userName;
}
