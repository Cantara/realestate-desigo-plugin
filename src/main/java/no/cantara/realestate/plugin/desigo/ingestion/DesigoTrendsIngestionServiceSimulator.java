package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.TrendsIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.sensors.SensorId;

import java.util.ArrayList;
import java.util.List;

public class DesigoTrendsIngestionServiceSimulator implements TrendsIngestionService {

    private boolean isInitialized = false;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;

    private List<SensorId> sensorIds = new ArrayList<>();
    private ObservationListener observationListener;
    private NotificationListener notificationListener;

    @Override
    public void ingestTrends() {
        int max = 35;
        int min = 10;
        for (SensorId sensorId : sensorIds) {
            ObservedValue observedValue = new ObservedValue(sensorId, ((Math.random() * (max - min)) + min));
            observationListener.observedValue(observedValue);
        }
    }


    @Override
    public String getName() {
        return "SimulatorDesigoTrendsIngestionService";
    }

    @Override
    public boolean initialize(PluginConfig properties) {
        isInitialized = true;
        return true;
    }

    @Override
    public void openConnection(ObservationListener observationListener, NotificationListener notificationListener) {
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
    }

    @Override
    public void closeConnection() {

    }

    @Override
    public void addSubscriptions(List<SensorId> list) {
        sensorIds.addAll(list);
    }

    @Override
    public void addSubscription(SensorId sensorId) {
        sensorIds.add(sensorId);
    }

    @Override
    public void removeSubscription(SensorId sensorId) {
        sensorIds.remove(sensorId);
    }

    @Override
    public long getSubscriptionsCount() {
        return sensorIds.size();
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public boolean isHealthy() {

        return (isInitialized && observationListener != null);
    }

    @Override
    public long getNumberOfMessagesImported() {
        return numberOfMessagesImported;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }

}
