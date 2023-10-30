package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoTrendSample;
import no.cantara.realestate.plugin.desigo.trends.AzureTrendsLastUpdatedService;
import no.cantara.realestate.plugin.desigo.trends.TrendsLastUpdatedRepository;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.TrendsIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.security.LogonFailedException;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoTrendsIngestionService implements TrendsIngestionService {
    private static final Logger log = getLogger(DesigoTrendsIngestionService.class);
    private ObservationListener observationListener;
    private NotificationListener notificationListener;
    private DesigoApiClientRest desigoApiClient;

    AzureTrendsLastUpdatedService azureTrendsLastUpdatedService;
    private PluginConfig config;
    private URI apiUri;
    private ArrayList<SensorId> sensorIds;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;
    private boolean isInitialized = false;
    private TrendsLastUpdatedRepository trendsLastUpdatedRepository;

    public DesigoTrendsIngestionService(DesigoApiClientRest desigoApiClient) {
        sensorIds = new ArrayList<>();
        this.desigoApiClient = desigoApiClient;
    }

    /**
     * Used for testing
     *
     * @param config
     * @param observationListener
     * @param notificationListener
     * @param desigoApiClient
     */
    protected DesigoTrendsIngestionService(PluginConfig config, ObservationListener observationListener, NotificationListener notificationListener, DesigoApiClientRest desigoApiClient, AzureTrendsLastUpdatedService azureTrendsLastUpdatedService) {
        this.config = config;
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
        this.desigoApiClient = desigoApiClient;
        this.azureTrendsLastUpdatedService = azureTrendsLastUpdatedService;
    }
    @Override
    public void ingestTrends() {
        azureTrendsLastUpdatedService.readLastUpdated();
        for (SensorId sensorId : sensorIds) {
            try {
                String trendId = ((DesigoSensorId) sensorId).getTrendId();
                Instant lastObservedAt = azureTrendsLastUpdatedService.getLastUpdatedAt((DesigoSensorId) sensorId);
                Set<DesigoTrendSample> trendSamples = desigoApiClient.findTrendSamplesByDate(trendId, -1, -1, lastObservedAt.minusSeconds(600));
                for (DesigoTrendSample trendValue : trendSamples) {
                    ObservedValue observedValue = new ObservedValue(sensorId, trendValue.getValue());
                    observedValue.setObservedAt(trendValue.getObservedAt());
                    observationListener.observedValue(observedValue);
                    numberOfMessagesImported++;
                    azureTrendsLastUpdatedService.setLastUpdatedAt((DesigoSensorId) sensorId, trendValue.getObservedAt());
                }
            } catch (LogonFailedException e) {
                numberOfMessagesFailed++;
                azureTrendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                log.error("Failed to logon to Desigo CC API {} using username {}", apiUri, config.asString("sd.api.username", "admin"), e);
                throw new DesigoCloudConnectorException("Could not ingest trends for " + getName() + " Logon failed to " + apiUri + ", using username: " + config.asString("sd.api.username", "admin"), e);
            } catch (URISyntaxException e) {
                numberOfMessagesFailed++;
                azureTrendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public String getName() {
        return "DesigoTrendsIngestionService";
    }

    @Override
    public boolean initialize(PluginConfig pluginConfig) {
        this.config = pluginConfig;
        String apiUrl = config.asString("sd.api.url", "http://<localhost>:<port>");
        apiUri = URI.create(apiUrl);
        desigoApiClient = new DesigoApiClientRest(apiUri);
        if (azureTrendsLastUpdatedService == null) {
            trendsLastUpdatedRepository = new TrendsLastUpdatedRepository();
            azureTrendsLastUpdatedService = new AzureTrendsLastUpdatedService(config, trendsLastUpdatedRepository);
        }
        try {
            desigoApiClient.logon();
        } catch (LogonFailedException lfe) {
            String username = config.asString("sd.api.username", "admin");
            String password = config.asString("sd.api.password", "admin");
            try {
                desigoApiClient.openConnection(username, password, notificationListener);
            } catch (LogonFailedException e) {
                log.error("Failed to logon to Desigo CC API {} using username {}", apiUri, username, e);
                throw new DesigoCloudConnectorException("Could not open connection for " + getName() + " Logon failed to " + apiUri + ", using username: " + username, e);
            }
        }

//        tableSensorImporter = new DesigoTableSensorImporter(config);
        isInitialized = true;
        return true;
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

    public PluginConfig getConfig() {
        return config;
    }
}
