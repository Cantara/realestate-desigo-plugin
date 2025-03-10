package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.automationserver.BasClient;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedTrendedValue;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.observations.TrendSample;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.SdClientSimulator;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static no.cantara.realestate.plugin.desigo.utils.DesigoConstants.auditLog;
import static no.cantara.realestate.utils.StringUtils.hasValue;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoTrendsIngestionService implements TrendsIngestionService {
    private static final Logger log = getLogger(no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionService.class);
    public static final String BAS_URL_KEY = "sd.api.url";
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
    private boolean isHealthy;
    private Instant lastObservationReceievedAt = null;


    /**
     * Used for testing
     *
     * @param config
     * @param observationListener
     * @param notificationListener
     * @param desigoApiClient
     */
    public DesigoTrendsIngestionService(PluginConfig config, ObservationListener observationListener, NotificationListener notificationListener, BasClient desigoApiClient, TrendsLastUpdatedService trendsLastUpdatedService) {
        sensorIds = new ArrayList<>();
        this.config = config;
        if (config == null || observationListener == null || notificationListener == null || desigoApiClient == null || trendsLastUpdatedService == null) {
            throw new DesigoCloudConnectorException("Failed to create DesigoTrendsIngestionService. " +
                    "One or more of the parameters are null. config has value: " + (config != null)
                    + ", observationListener: " + observationListener
                    + ", notificationListener: " + notificationListener
                    + ", desigoApiClient: " + desigoApiClient
                    + ", trendsLastUpdatedService: "
                    + trendsLastUpdatedService);
        }
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
        this.desigoApiClient = desigoApiClient;
        this.trendsLastUpdatedService = trendsLastUpdatedService;

    }

    @Override
    public void ingestTrends() {
        try {
            trendsLastUpdatedService.readLastUpdated();
        } catch (NullPointerException npe) {
            isHealthy = false;
            DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to read last updated. TrendsLastUpdatedService is null. That service must be injected on creation of DesigoTrendsIngestionService.", npe);
            log.warn(de.getMessage());
            throw de;
        }
        List<DesigoSensorId> updatedSensors = new ArrayList<>();
        List<DesigoSensorId> failedSensors = new ArrayList<>();
        for (SensorId sensorId : sensorIds) {
            String trendId = ((DesigoSensorId) sensorId).getTrendId();
            if (hasValue(trendId)) {
                auditLog.trace("Ingest__TrendFindSamples__{}__{}__{}", trendId, sensorId.getClass(), sensorId.getId());
                try {
                    Instant lastObservedAt = trendsLastUpdatedService.getLastUpdatedAt((DesigoSensorId) sensorId);
                    auditLog.trace("Ingest__TrendLastUpdatedAt__{}__{}__{}__{}", trendId, sensorId.getClass(), sensorId.getId(), lastObservedAt);
                    if (lastObservedAt == null) {
                        lastObservedAt = getDefaultLastObservedAt();
                    }
                    Set<? extends TrendSample> trendSamples = desigoApiClient.findTrendSamplesByDate(trendId, -1, -1, lastObservedAt.minusSeconds(600));
                    isHealthy = true;
                    if (trendSamples != null && trendSamples.size() > 0) {
                        updateWhenLastObservationReceived();
                        auditLog.trace("Ingest__TrendSamplesFound__{}__{}__{}__{}", trendId, sensorId.getClass(), sensorId.getId(), trendSamples.size());
                    } else {
                        auditLog.trace("Ingest__TrendSamplesFound__{}__{}__{}__{}", trendId, sensorId.getClass(), sensorId.getId(), 0);
                    }
                    for (TrendSample trendValue : trendSamples) {
//                        trendValue = (DesigoTrendSample) trendValue;
                        ObservedValue observedValue = new ObservedTrendedValue(sensorId, trendValue.getValue());
                        if (trendValue.getObservedAt() != null) {
                            observedValue.setObservedAt(trendValue.getObservedAt());
                        }
                        auditLog.trace("Ingest__TrendObserved__{}__{}__{}__{}__{}", trendId, observedValue.getClass(), observedValue.getSensorId().getId(), observedValue.getValue(), observedValue.getObservedAt());
                        observationListener.observedValue(observedValue);
                        addMessagesImportedCount();
                        trendsLastUpdatedService.setLastUpdatedAt((DesigoSensorId) sensorId, trendValue.getObservedAt());
                    }
                    updatedSensors.add((DesigoSensorId) sensorId);
                } catch (LogonFailedException e) {
                    addMessagesFailedCount();
                    trendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                    failedSensors.add((DesigoSensorId) sensorId);
                    log.error("Failed to logon to Desigo CC API {} using username {}", apiUrl, config.asString("sd.api.username", "admin"), e);
                    throw new DesigoCloudConnectorException("Could not ingest trends for " + getName() + " Logon failed to " + apiUrl + ", using username: " + config.asString("sd.api.username", "admin"), e);
                } catch (URISyntaxException e) {
                    addMessagesFailedCount();
                    trendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                    failedSensors.add((DesigoSensorId) sensorId);
                    auditLog.trace("Ingest__Failed__TrendId__{}__sensorId__{}. Reason {}", trendId, sensorId, e.getMessage());
                } catch (DesigoCloudConnectorException dce) {
                    addMessagesFailedCount();
                    trendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                    failedSensors.add((DesigoSensorId) sensorId);
                    log.debug("Failed to ingest trends for TrendId {} sensorId {}.", trendId, sensorId, dce);
                    auditLog.trace("Ingest__TrendImportFailed__{}__{}__{}__{}", trendId, sensorId.getId(), ((DesigoSensorId) sensorId).getDesigoPropertyId(), dce.getMessage());
                } catch (Exception e) {
                    addMessagesFailedCount();
                    trendsLastUpdatedService.setLastFailedAt((DesigoSensorId) sensorId, Instant.now());
                    failedSensors.add((DesigoSensorId) sensorId);
                    log.debug("Failed to ingest trends for sensorId {}.", sensorId, e);
                }
            } else {
                auditLog.trace("Ingest__TrendIdMissing__{}__{}__{}__{}__{}", sensorId.getClass(), sensorId.getId(), ((DesigoSensorId) sensorId).getDesigoPropertyId());
            }
        }
        trendsLastUpdatedService.persistLastUpdated(updatedSensors);
        trendsLastUpdatedService.persistLastFailed(failedSensors);

    }

    protected Instant getDefaultLastObservedAt() {
        return Instant.now().minus(30, ChronoUnit.DAYS);
    }

    @Override
    public String getName() {
        return "DesigoTrendsIngestionService";
    }

    @Override
    public boolean initialize(PluginConfig pluginConfig) {
        log.trace("DesigoTrendsIngestionService.initialize");
        this.config = pluginConfig;
        apiUrl = config.asString(BAS_URL_KEY, null);
        if (!hasValue(apiUrl)) {
            throw new DesigoCloudConnectorException("Failed to initialize DesigoTrendsIngestionService. Desigo." + BAS_URL_KEY + " is null or empty. Please set this property.");
        }
        boolean initializationOk = false;
        if (desigoApiClient != null && !desigoApiClient.isHealthy()) {
            if (desigoApiClient instanceof DesigoApiClientRest) {
                log.info("DesigoApiClient is null or unhealty. Creating a new one. {}", desigoApiClient);
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
                ((SdClientSimulator) desigoApiClient).openConnection(null, null, notificationListener);
                initializationOk = true;
            }
        } else if (desigoApiClient == null) {
            log.warn("DesigoApiClient is null. {}", desigoApiClient);
            initializationOk = false;
        } else if (desigoApiClient.isHealthy()) {
            log.trace("DesigoApiClient is healthy. {}", desigoApiClient);
            initializationOk = true;
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
        return isHealthy;
    }

    @Override
    public long getNumberOfMessagesImported() {
        return numberOfMessagesImported;
    }

    @Override
    public long getNumberOfMessagesFailed() {
        return numberOfMessagesFailed;
    }
    synchronized void addMessagesImportedCount() {
        numberOfMessagesImported++;
    }

    synchronized void addMessagesFailedCount() {
        numberOfMessagesFailed++;
    }

    protected PluginConfig getConfig() {
        return config;
    }

    protected BasClient getDesigoApiClientRest() {
        return desigoApiClient;
    }

    protected synchronized void updateWhenLastObservationReceived() {
        lastObservationReceievedAt = Instant.ofEpochMilli(System.currentTimeMillis());
    }

    @Override
    public Instant getWhenLastMessageImported() {
        return lastObservationReceievedAt;
    }

    protected void setWhenLastObservationReceivedAt(Instant lastObservationReceievedAt) {
        this.lastObservationReceievedAt = lastObservationReceievedAt;
    }
}
