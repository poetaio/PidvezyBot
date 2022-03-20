package repositories;

import models.QueueTrip;
import models.hibernate.Trip;
import models.hibernate.User;
import models.utils.TripStatus;
import org.hibernate.Session;
import utils.HibernateUtil;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TripRepository {
    private final UserRepository userRepository;

    public TripRepository() {
        userRepository = new UserRepository();
    }

    public Trip getTripWithDefault(Session session, UUID tripId) {
        Trip trip = session.get(Trip.class, tripId);
        if (trip == null) {
            trip = new Trip(tripId);
            // no need to generate id
            session.persist(trip);
        }
        return trip;
    }

    public void initTrip(UUID tripId, long userId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        User passenger = userRepository.getWithDefault(session, userId);
        session.persist(new Trip(tripId, passenger));

        session.getTransaction().commit();
    }

    public void setAddress(UUID tripId, String address) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getTripWithDefault(session, tripId).setAddress(address);

        session.getTransaction().commit();
    }

    public void setDetails(UUID tripId, String details) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getTripWithDefault(session, tripId).setDetails(details);

        session.getTransaction().commit();
    }

    public void createNewTrip(QueueTrip newTrip) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Trip trip = getTripWithDefault(session, newTrip.getTripId());
        User passenger = userRepository.getWithDefault(session, newTrip.getPassengerChatId());
        trip.setTripStatus(TripStatus.INACTIVE);
        trip.setAddress(newTrip.getAddress());
        trip.setDetails(newTrip.getDetails());
        trip.setPassenger(passenger);

        session.getTransaction().commit();
    }

    // finished creating queue or look for driver once again
    public void addTripToQueue(UUID tripId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getTripWithDefault(session, tripId).setTripStatus(TripStatus.IN_QUEUE);

        session.getTransaction().commit();
    }

    public void setDriverViewOnTrip(UUID tripId, long userId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Trip trip = getTripWithDefault(session, tripId);
        trip.getListOfViewDriver().add(userRepository.getWithDefault(session, userId));

        session.getTransaction().commit();
    }

    public void removeDriverViewFromTrip(UUID tripId, long userId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Trip trip = getTripWithDefault(session, tripId);
        List<User> currentList = trip.getListOfViewDriver();
        trip.setListOfViewDriver(
                currentList.stream()
                        .filter(x -> x.getUserId() != userId)
                        .collect(Collectors.toList())
        );

        session.getTransaction().commit();
    }

    public void setDriverTookTrip(UUID tripId, long userId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Trip trip = getTripWithDefault(session, tripId);
        if (trip.getListOfViewDriver() != null)
            trip.getListOfViewDriver().clear();
        trip.setTripStatus(TripStatus.TAKEN);
        trip.setTakenByDriver(userRepository.getWithDefault(session, userId));

        session.getTransaction().commit();
    }

    // set status to inactive
    public void unsetDriverTookTrip(UUID tripId, long userId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Trip trip = getTripWithDefault(session, tripId);
        if (trip.getTakenByDriver() != null && trip.getTakenByDriver().getUserId() == userId) {
            trip.setTakenByDriver(null);
            trip.setTripStatus(TripStatus.INACTIVE);
        }

        session.getTransaction().commit();
    }

    public void deactivateTrip(UUID tripId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getTripWithDefault(session, tripId).setTripStatus(TripStatus.INACTIVE);

        session.getTransaction().commit();
    }

    public void cancelTrip(UUID tripId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        getTripWithDefault(session, tripId).setTripStatus(TripStatus.CANCELED);

        session.getTransaction().commit();
    }

    public void finishTrip(UUID tripId) {
        Session session = HibernateUtil.getSession();
        session.beginTransaction();

        Trip trip = getTripWithDefault(session, tripId);
        trip.getListOfViewDriver().clear();
        trip.setTripStatus(TripStatus.FINISHED);
        trip.setFinishedByDriver(trip.getTakenByDriver());
        trip.setTakenByDriver(null);

        session.getTransaction().commit();
    }
}
