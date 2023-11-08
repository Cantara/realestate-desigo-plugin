package no.cantara.realestate.plugin.desigo.trends;

import no.cantara.realestate.sensors.desigo.DesigoSensorId;

import java.time.Instant;
import java.util.List;

public interface TrendsLastUpdatedService {
    void readLastUpdated();
    Instant getLastUpdatedAt(DesigoSensorId sensorId);
    void setLastUpdatedAt(DesigoSensorId sensorId, Instant lastUpdatedAt);
    void setLastFailedAt(DesigoSensorId sensorId, Instant lastFailedAt);

    void persistLastUpdated(List<DesigoSensorId> sensorIds);

    void persistLastFailed(List<DesigoSensorId> sensorIds);
    boolean isHealthy();
    List<String> getErrors();
}
