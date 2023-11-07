package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.automationserver.BasClient;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.observations.TrendSample;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoTrendSample;
import no.cantara.realestate.plugin.desigo.automationserver.SdClientSimulator;
import no.cantara.realestate.plugin.desigo.trends.AzureTrendsLastUpdatedService;
import no.cantara.realestate.plugin.desigo.trends.TrendsLastUpdatedRepository;
import no.cantara.realestate.plugin.desigo.trends.TrendsLastUpdatedService;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.TrendsIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.security.LogonFailedException;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoTrendsIngestionService implements TrendsIngestionService {
    private static final Logger log = getLogger(no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionService.class);
    private ObservationListener observationListener;
    private NotificationListener notificationListener;
    private BasClient desigoApiClient;

    private TrendsLastUpdatedService trendsLastUpdatedService;
    private PluginConfig config;
    private ArrayList<SensorId> sensorIds;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;
    private boolean isInitialized = false;
    private String apiUrl;

    public DesigoTrendsIngestionService(BasClient desigoApiClient, TrendsLastUpdatedService trendsLastUpdatedService) {
        sensorIds = new ArrayList<>();
        this.desigoApiClient = desigoApiClient;
        this.trendsLastUpdatedService = trendsLastUpdatedService;
    }

    /**
     * Used for testing
     *
     * @param config
     * @param observationListener
     * @param notificationListener
     * @param desigoApiClient
     */
    protected DesigoTrendsIngestionService(PluginConfig config, ObservationListener observationListener, NotificationListener notificationListener, DesigoApiClientRest desigoApiClient, TrendsLastUpdatedService trendsLastUpdatedService) {
        this.config = config;
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
        this.desigoApiClient = desigoApiClient;
        this.trendsLastUpdatedService = trendsLastUpdatedService;
    }

    @Override
    public void ingestTrends() {
        trendsLastUpdatedService.readLastUpdated();
        List<DesigoSensorId> updatedSensors = new ArrayList<>();
        List<DesigoSensorId> failedSensors = new ArrayList<>();
        for (SensorId sensorId : sensorIds) {
            try {
                String trendId = ((DesigoSensorId) sensorId).getTrendId();
                Instant lastObservedAt = trendsLastUpdatedService.getLastUpdatedAt((DesigoSensorId) sensorId);
                if (lastObservedAt == null) {
                    lastObservedAt = Instant.now();
                }
                Set<? extends TrendSample> trendSamples = desigoApiClient.findTrendSamplesByDate(trendId, -1, -1, lastObservedAt.minusSeconds(600));
                for (TrendSample trendValue : trendSamples) {
                    trendValue = (DesigoTrendSample) trendValue;
                    if (trendValue.getObservedAt() == null) {
                        throw new RuntimeException("Not allowed");
                    }
                    ObservedValue observedValue = new ObservedValue(sensorId, trendValue.getValue());
                    observedValue.setObservedAt(trendValue.getObservedAt());
                    observationListener.observedValue(observedValue);
                    numberOfMessagesImported++;
                    trendsLastUpdatedService.setLastUpdatedAt((DesigoSensorId) sensorId, trendValue.getObservedAt());
                }
                updatedSensors.add((DesigoSensorId) sensorId);
            } catch (LogonFailedException e) {
                numberOfMessagesFailed++;
                trendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                failedSensors.add((DesigoSensorId) sensorId);
                log.error("Failed to logon to Desigo CC API {} using username {}", apiUrl, config.asString("sd.api.username", "admin"), e);
                throw new DesigoCloudConnectorException("Could not ingest trends for " + getName() + " Logon failed to " + apiUrl + ", using username: " + config.asString("sd.api.username", "admin"), e);
            } catch (URISyntaxException e) {
                numberOfMessagesFailed++;
                trendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                failedSensors.add((DesigoSensorId) sensorId);
                throw new RuntimeException(e);
            }
        }
        trendsLastUpdatedService.persistLastUpdated(updatedSensors);
        trendsLastUpdatedService.persistLastFailed(failedSensors);

    }

    @Override
    public String getName() {
        return "DesigoTrendsIngestionService";
    }

    @Override
    public boolean initialize(PluginConfig pluginConfig) {
        this.config = pluginConfig;
        apiUrl = config.asString("sd.api.url", "http://localhost:8080");
        boolean initializationOk = false;
        if (desigoApiClient != null && !desigoApiClient.isHealthy()) {
            if (desigoApiClient instanceof DesigoApiClientRest) {

                String username = config.asString("sd.api.username", "admin");
                String password = config.asString("sd.api.password", "admin");
                try {
                    ((DesigoApiClientRest) desigoApiClient).openConnection(username, password, notificationListener);
                } catch (LogonFailedException e) {

                    log.error("Failed to logon to Desigo CC API {} using username {}", apiUrl, username, e);
                    throw new DesigoCloudConnectorException("Could not open connection for " + getName() + " Logon failed to " + apiUrl + ", using username: " + username, e);
                }
                initializationOk = true;
            } else if (desigoApiClient instanceof SdClientSimulator) {
                ((SdClientSimulator)desigoApiClient).openConnection(null, null, notificationListener);
                initializationOk = true;
            }
        }
        isInitialized = initializationOk;

        return initializationOk;
    }

    @Override
    public void openConnection(ObservationListener observationListener, NotificationListener notificationListener) {
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
        if (!isInitialized) {
            throw new RuntimeException("Not initialized. Please call initialize() first.");
        }
    }

    @Override
    public void closeConnection() {
        desigoApiClient = null;
    }

    @Override
    public void addSubscriptions(List<SensorId> list) {
        if (sensorIds == null) {
            sensorIds = new ArrayList<>();
        }
        sensorIds.addAll(list);
    }

    @Override
    public void addSubscription(SensorId sensorId) {
        if (sensorIds == null) {
            sensorIds = new ArrayList<>();
        }
        sensorIds.add(sensorId);
    }

    @Override
    public void removeSubscription(SensorId sensorId) {
        if (sensorIds != null) {
            sensorIds.remove(sensorId);
        }
    }

    @Override
    public long getSubscriptionsCount() {
        if (sensorIds != null) {
            return sensorIds.size();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public long getNumberOfMessagesImported() {
        return numberOfMessagesImported;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }

    protected PluginConfig getConfig() {
        return config;
    }

    protected BasClient getDesigoApiClientRest() {
        return desigoApiClient;
    }
}
