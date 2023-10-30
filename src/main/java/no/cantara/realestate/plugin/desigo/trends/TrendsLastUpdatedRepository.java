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
            setLastUpdated(id, lastUpdated);
        }
    }

    public void updateFailedIfNullOrNewer(DesigoSensorId id, Instant lastFailed) {
        Instant lastFailedStored = trendsLastFailed.get(id);
        if (lastFailedStored == null || lastFailed.isAfter(lastFailedStored)) {
            setLastFailed(id, lastFailed);
        }
    }

    private void setLastFailed(DesigoSensorId id, Instant lastFailed) {
        trendsLastFailed.put(id, lastFailed);
    }

    public void setLastUpdated(DesigoSensorId id, Instant lastUpdated) {
        trendsLastUpdated.put(id, lastUpdated);
    }

    public Map<DesigoSensorId, Instant> getTrendsLastUpdated() {
        return trendsLastUpdated;
    }

    public Map<DesigoSensorId, Instant> getTrendsLastFailed() {
        return trendsLastFailed;
    }

    public long getSize() {
        return trendsLastUpdated.size();
    }
}
