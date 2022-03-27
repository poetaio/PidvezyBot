package bots.pidvezy_group_bot.trip_update_handler;

import bots.pidvezy_group_bot.trip_update_handler.new_trip.NewTripConsumer;
import bots.pidvezy_group_bot.trip_update_handler.new_trip.NewTripProducer;
import bots.pidvezy_group_bot.trip_update_handler.new_trip.OnNewTrip;
import bots.pidvezy_group_bot.trip_update_handler.remove_trip.OnRemoveTrip;
import bots.pidvezy_group_bot.trip_update_handler.remove_trip.RemoveTripConsumer;
import bots.pidvezy_group_bot.trip_update_handler.remove_trip.RemoveTripProducer;
import lombok.SneakyThrows;
import models.QueueTrip;
import models.TakenTrip;
import models.dao.SendTripDao;
import services.event_service.utils.EventListener;
import services.event_service.utils.Events;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class TripUpdateManager implements EventListener {
    private final NewTripProducer newTripProducer;
    private final RemoveTripProducer removeTripProducer;

    public TripUpdateManager(OnNewTrip onNewTrip, OnRemoveTrip onRemoveTrip) {
        ConcurrentTripQueue newTripQueue = new ConcurrentTripQueue(100);
        ConcurrentTripQueue removeTripQueue = new ConcurrentTripQueue(100);

        newTripProducer = new NewTripProducer(newTripQueue);
        removeTripProducer = new RemoveTripProducer(removeTripQueue);

        new Thread(new NewTripConsumer(onNewTrip, newTripQueue)).start();
        new Thread(new RemoveTripConsumer(onRemoveTrip, removeTripQueue)).start();
    }

    @SneakyThrows
    @Override
    public void update(String eventName, Object o) {
        if (Objects.equals(eventName, Events.NEW_TRIP_EVENT)) {
            if (o == null || !o.getClass().equals(SendTripDao.class)) {
                System.out.println("Cannot handle event due to cast exception");
                return;
            }
            CompletableFuture.runAsync(() -> newTripProducer.produce((SendTripDao) o));
        } else if (Objects.equals(eventName, Events.REMOVE_TRIP_EVENT)) {
            if (o == null || !o.getClass().equals(SendTripDao.class)) {
                System.out.println("Cannot handle event due to cast exception");
                return;
            }
            CompletableFuture.runAsync(() -> removeTripProducer.produce((SendTripDao) o));
        }
    }
}
