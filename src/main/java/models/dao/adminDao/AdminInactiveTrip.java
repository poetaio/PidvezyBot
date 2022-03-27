package models.dao.adminDao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class AdminInactiveTrip {
    private UserDao passenger;
    private String address;
    private String details;
}
