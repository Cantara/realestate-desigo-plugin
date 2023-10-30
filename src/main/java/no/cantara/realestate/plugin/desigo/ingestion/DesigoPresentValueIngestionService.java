package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoPresentValue;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.security.LogonFailedException;
import no.cantara.realestate.sensors.SensorId;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoPresentValueIngestionService implements PresentValueIngestionService {
    private static final Logger log = getLogger(DesigoPresentValueIngestionService.class);
    private PluginConfig config;
    private ObservationListener observationListener;
    private NotificationListener notificationListener;
    private boolean isInitialized = false;
    private DesigoApiClientRest desigoApiClient;
    private URI apiUri;
    private ArrayList<SensorId> sensorIds;
    private long numberOfMessagesImported = 0;
    private long numberOfMessagesFailed = 0;

    public DesigoPresentValueIngestionService(DesigoApiClientRest desigoApiClient) {
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
    protected DesigoPresentValueIngestionService(PluginConfig config, ObservationListener observationListener, NotificationListener notificationListener, DesigoApiClientRest desigoApiClient) {
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
                DesigoPresentValue presentValue = desigoApiClient.findPresentValue(sensorId);
                ObservedValue observedValue = new ObservedValue(sensorId, presentValue.getValue());
                observedValue.setObservedAt(presentValue.getObservedAt());
                observedValue.setReliable(presentValue.getReliable());
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
        this.config = config;
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
