package no.cantara.realestate.plugin.desigo.trends;

import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class InMemoryTrendsLastUpdatedService implements TrendsLastUpdatedService {
    private static final Logger log = getLogger(InMemoryTrendsLastUpdatedService.class);
    Map<DesigoSensorId, Instant> lastUpdated;
    Map<DesigoSensorId, Instant> lastFailed;

    public InMemoryTrendsLastUpdatedService() {
        lastUpdated = new HashMap<>();
        lastFailed = new HashMap<>();
    }

    public InMemoryTrendsLastUpdatedService(Map<DesigoSensorId, Instant> lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public void readLastUpdated() {

    }

    @Override
    public Instant getLastUpdatedAt(DesigoSensorId sensorId) {
        return lastUpdated.get(sensorId);
    }

    @Override
    public void setLastUpdatedAt(DesigoSensorId sensorId, Instant lastUpdatedAt) {
        if ( sensorId != null && lastUpdatedAt != null) {
            lastUpdated.put(sensorId, lastUpdatedAt);
        }
    }

    @Override
    public void setLastFailedAt(DesigoSensorId sensorId, Instant lastFailedAt) {
        if(sensorId != null && lastFailedAt != null) {
            lastFailed.put(sensorId, lastFailedAt);
        }
    }

    @Override
    public void persistLastUpdated(List<DesigoSensorId> sensorIds) {
        log.info("Simulating persisting last updated for {} sensors", sensorIds.size());
    }

    @Override
    public void persistLastFailed(List<DesigoSensorId> sensorIds) {
        log.info("Simulating persisting last failed for {} sensors", sensorIds.size());
    }
}
