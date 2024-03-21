package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.automationserver.BasClient;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedPresentValue;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.observations.PresentValue;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.SdClientSimulator;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.security.LogonFailedException;
import no.cantara.realestate.sensors.SensorId;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static no.cantara.realestate.plugin.desigo.utils.DesigoConstants.auditLog;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoPresentValueIngestionService implements PresentValueIngestionService {
    private static final Logger log = getLogger(DesigoPresentValueIngestionService.class);
    private PluginConfig config;
    private ObservationListener observationListener;
    private NotificationListener notificationListener;
    private boolean isInitialized = false;
    private BasClient desigoApiClient;
    private URI apiUri;
    private ArrayList<SensorId> sensorIds;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;

    /**
     * Used for testing
     *
     * @param config
     * @param observationListener
     * @param notificationListener
     * @param desigoApiClient
     */
    public DesigoPresentValueIngestionService(PluginConfig config, ObservationListener observationListener, NotificationListener notificationListener, BasClient desigoApiClient) {
        sensorIds = new ArrayList<>();
        if (config == null || observationListener == null || notificationListener == null || desigoApiClient == null) {
            throw new DesigoCloudConnectorException("Failed to create DesigoTrendsIngestionService. " +
                    "One or more of the parameters are null. config: " + config
                    + ", observationListener: " + observationListener
                    + ", notificationListener: " + notificationListener
                    + ", desigoApiClient: " + desigoApiClient);
        }
        this.config = config;
        this.observationListener = observationListener;
        this.notificationListener = notificationListener;
        this.desigoApiClient = desigoApiClient;
    }

    @Override
    public void ingestPresentValues() {
        log.debug("Ingesting present values from Desigo CC API {} using sensorIds {}", apiUri, sensorIds);
        for (SensorId sensorId : sensorIds) {
            try {
                auditLog.trace("Ingest__PresentValueFindValue__{}__{}", sensorId.getId(), sensorId.getClass());
                PresentValue presentValue = desigoApiClient.findPresentValue(sensorId);
                auditLog.trace("Ingest__PresentValue__{}__{}__{}__{}", sensorId.getId(), presentValue.getClass(),presentValue.getValue(),presentValue.getObservedAt());
                ObservedValue observedValue = new ObservedPresentValue(sensorId, presentValue.getValue());
                if (presentValue.getObservedAt() != null) {
                    observedValue.setObservedAt(presentValue.getObservedAt());
                } else {
                    log.trace("PresentValue.getObservedAt() is null. Setting to now");
                    observedValue.setObservedAt(Instant.now());
                }
                if (presentValue.getReliable() != null) {
                    observedValue.setReliable(presentValue.getReliable());
                }
                auditLog.trace("Ingest__PresentValue__Observed__{}__{}__{}__{}__{}", sensorId.getId(), observedValue.getClass(),presentValue.getValue(),presentValue.getObservedAt(), observedValue);
                observationListener.observedValue(observedValue);
            } catch (URISyntaxException e) {
                log.error("Failed to get sensor observations from Desigo CC API {} using sensorId {}", apiUri, sensorId, e);
                throw new DesigoCloudConnectorException("Could not get sensor observations from " + apiUri + ". URI is illegal", e);
            } catch (LogonFailedException e) {
                log.error("Failed to get sensor observations from Desigo CC API {} as logon failed. Please try to  calling " +
                        "closeConnection(), then openConnection().", apiUri, e);
                throw e;
            }
        }

    }

    @Override
    public String getName() {
        return "DesigoPresentValueIngestionService";
    }

    @Override
    public boolean initialize(PluginConfig config) {
        log.trace("DesigoPresentValueIngestionService.initialize");
        this.config = config;
        boolean initializationOk = false;
        if (desigoApiClient != null && !desigoApiClient.isHealthy()) {
            if (desigoApiClient instanceof DesigoApiClientRest) {
                String username = config.asString("sd.api.username", "admin");
                String password = config.asString("sd.api.password", "admin");
                try {
                    ((DesigoApiClientRest) desigoApiClient).openConnection(username, password, notificationListener);

                } catch (LogonFailedException e) {
                    log.error("Failed to logon to Desigo CC API {} using username {}", apiUri, username, e);
                    throw new DesigoCloudConnectorException("Could not open connection for " + getName() + " Logon failed to " + apiUri + ", using username: " + username, e);
                }
                initializationOk = true;
            } else if (desigoApiClient instanceof SdClientSimulator) {
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
        observationListener = null;
        notificationListener = null;
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
}
