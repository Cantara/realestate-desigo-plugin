package no.cantara.realestate.plugin.desigo.trends;

import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class TrendsLastUpdatedRepository {
    private static final Logger log = getLogger(TrendsLastUpdatedRepository.class);

    private Map<DesigoSensorId, Instant> trendsLastUpdated = new HashMap<>();
    private Map<DesigoSensorId, Instant> trendsLastFailed = new HashMap<>();

    public void updateUpdatedIfNullOrNewer(DesigoSensorId id, Instant lastUpdated) {
        Instant lastUpdatedStored = trendsLastUpdated.get(id);
        if (lastUpdatedStored == null || lastUpdated.isAfter(lastUpdatedStored)) {
            addLastUpdated(id, lastUpdated);
        }
    }

    public void updateFailedIfNullOrNewer(DesigoSensorId id, Instant lastFailed) {
        Instant lastFailedStored = trendsLastFailed.get(id);
        if (lastFailedStored == null || lastFailed.isAfter(lastFailedStored)) {
            addLastFailed(id, lastFailed);
        }
    }

    public void addLastFailed(DesigoSensorId id, Instant lastFailed) {
        trendsLastFailed.put(id, lastFailed);
    }

    public void addLastUpdated(DesigoSensorId id, Instant lastUpdated) {
        trendsLastUpdated.put(id, lastUpdated);
    }

    public Map<DesigoSensorId, Instant> getTrendsLastUpdated() {
        return trendsLastUpdated;
    }

    public Map<DesigoSensorId, Instant> getTrendsLastFailed() {
        return trendsLastFailed;
    }

    public long countLastUpdatedSensors() {
        return trendsLastUpdated.size();
    }

    public long countLastFailedSensors() {
        return trendsLastFailed.size();
    }

    public void clear() {
        trendsLastUpdated.clear();
        trendsLastFailed.clear();
    }

    public Instant getLastUpdated(DesigoSensorId sensorId) {
        return trendsLastUpdated.get(sensorId);
    }
}
