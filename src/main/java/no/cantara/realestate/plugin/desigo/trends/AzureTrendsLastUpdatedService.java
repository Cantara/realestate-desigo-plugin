package no.cantara.realestate.plugin.desigo.trends;

import no.cantara.realestate.azure.storage.AzureStorageTablesClient;
import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class AzureTrendsLastUpdatedService implements TrendsLastUpdatedService{
    private static final Logger log = getLogger(AzureTrendsLastUpdatedService.class);
    private final PluginConfig config;
    private final TrendsLastUpdatedRepository repository;

    private AzureTableClient lastUpdatedClient;
    private AzureTableClient lastFailedClient;
    private boolean isHealthy;
    private List<String> currentErrors = new ArrayList<>();


    public AzureTrendsLastUpdatedService(PluginConfig config, TrendsLastUpdatedRepository repository) {
        this.config = config;
        this.repository = repository;
        isHealthy = false;
    }

    /*
    Used for testing
     */
    public AzureTrendsLastUpdatedService(PluginConfig config, TrendsLastUpdatedRepository repository, AzureTableClient lastUpdatedClient, AzureTableClient lastFailedClient) {
        this.config = config;
        this.repository = repository;
        this.lastUpdatedClient = lastUpdatedClient;
        this.lastFailedClient = lastFailedClient;
        if (config != null && repository != null && lastUpdatedClient != null && lastFailedClient != null) {
            isHealthy = true;
        }
    }

    @Override
    public void readLastUpdated() {
        verifyOrInitializeAzureTableClients();
        if (lastUpdatedClient == null) {
            DesigoCloudConnectorException de = new DesigoCloudConnectorException("TrendsLastUpdatedService is null");
            log.warn(de.getMessage());
            currentErrors.add(Instant.now() + "--" + de.getMessage());
            isHealthy = false;
            throw de;
        }
        if (lastFailedClient == null) {
            DesigoCloudConnectorException de = new DesigoCloudConnectorException("TrendsLastFailedService is null");
            log.warn(de.getMessage());
            currentErrors.add(Instant.now() + "--" + de.getMessage());
            isHealthy = false;
            throw de;
        }
        isHealthy = true;
        String partitionKey = config.asString("trends.lastupdated.partitionKey", "Desigo");
        lastUpdatedClient.listRows(partitionKey).forEach(tableEntity -> {
            updateRepository(tableEntity);
        });
        isHealthy = true;
    }

    protected void updateRepository(Map<String, String> tableEntity) {
        String trendId = tableEntity.get("RowKey");
        String id = tableEntity.get("DigitalTwinSensorId");
        String desigoObjectId = tableEntity.get("DesigoId");
        String desigoPropertyId = tableEntity.get("DesigoPropertyId");
        String lastUpdatedAtString = tableEntity.get("LastUpdatedAt");
        Instant lastUpdatedAt = null;
        if (lastUpdatedAtString != null && ! lastUpdatedAtString.isEmpty()) {
            try {
                lastUpdatedAt = Instant.parse(lastUpdatedAtString);
            } catch (Exception e) {
                log.trace("Could not parse lastUpdatedAtString: {} for RowKey {}. objectId.propertyId {}.{}", lastUpdatedAtString, trendId, desigoObjectId, desigoPropertyId, e);
            }
        }
        DesigoSensorId sensorId = new DesigoSensorId(desigoObjectId, desigoPropertyId);
        sensorId.setId(id);
        sensorId.setTrendId(trendId);
        repository.addLastUpdated(sensorId, lastUpdatedAt);
    }

    void verifyOrInitializeAzureTableClients() {
        log.trace("verifyOrInitializeAzureTableClients");
        //trends.lastupdated.enabled=true
        //trends.lastupdated.tableName=lastupdated
        //trends.lastupdated.partitionKey=Desigo

        if (lastUpdatedClient == null) {
            lastUpdatedClient = createLastUpdatedTableClient(config);
        }
        if (lastFailedClient == null) {
            lastFailedClient = createLastFailedTableClient(config);
        }
    }

    public static AzureTableClient createLastUpdatedTableClient(PluginConfig config) {
        String connectionString = config.asString(AzureStorageTablesClient.CONNECTIONSTRING_KEY, null);
        String tableName = config.asString("trends.lastUpdated.tableName", null);
        AzureTableClient lastUpdatedClient = new AzureTableClient(connectionString, tableName);
        log.info("Initialized lastUpdatedClient {} with tableName {}", lastUpdatedClient, tableName);
        return lastUpdatedClient;
    }
    public static AzureTableClient createLastFailedTableClient(PluginConfig config) {
        String connectionString = config.asString(AzureStorageTablesClient.CONNECTIONSTRING_KEY, null);
        String tableName = config.asString("trends.lastFailed.tableName", null);
        AzureTableClient lastFailedClient = new AzureTableClient(connectionString, tableName);
        log.info("Initialized lastFailedClient {} with tableName {}", lastFailedClient, tableName);
        return lastFailedClient;
    }

    @Override
    public Instant getLastUpdatedAt(DesigoSensorId sensorId) {
        return repository.getLastUpdated(sensorId);
    }

    @Override
    public void setLastUpdatedAt(DesigoSensorId sensorId, Instant lastUpdatedAt) {
        repository.updateUpdatedIfNullOrNewer(sensorId, lastUpdatedAt);
    }

    @Override
    public void setLastFailedAt(DesigoSensorId sensorId, Instant lastFailedAt) {
        repository.updateUpdatedIfNullOrNewer(sensorId, lastFailedAt);
    }

    @Override
    public void persistLastUpdated(List<DesigoSensorId> sensorIds) {
        String partitionKey = config.asString("trends.lastupdated.partitionKey", "Desigo");
        for (DesigoSensorId sensorId : sensorIds) {
            Instant hei = repository.getLastUpdated(sensorId);
            Instant lastUpdatedAt = repository.getTrendsLastUpdated().get(sensorId);
            if (lastUpdatedAt != null) {
                String rowKey = sensorId.getTrendId();
                String id = sensorId.getId();
                String desigoObjectId = sensorId.getDesigoId();
                String desigoPropertyId = sensorId.getDesigoPropertyId();
                String lastUpdatedAtString = lastUpdatedAt.toString();
                Map<String, Object> properties = Map.of(
                        "DigitalTwinSensorId", id,
                        "DesigoId", desigoObjectId,
                        "DesigoPropertyId", desigoPropertyId,
                        "LastUpdatedAt", lastUpdatedAtString
                );
                lastUpdatedClient.updateRow(partitionKey, rowKey,properties);
            }
        }
    }

    @Override
    public void persistLastFailed(List<DesigoSensorId> sensorIds) {
        String partitionKey = config.asString("trends.lastupdated.partitionKey", "Desigo");
        for (DesigoSensorId sensorId : sensorIds) {
            Instant lastFailedAt = repository.getTrendsLastFailed().get(sensorId);
            if (lastFailedAt != null) {
                String rowKey = sensorId.getTrendId();
                String id = sensorId.getId();
                String desigoObjectId = sensorId.getDesigoId();
                String desigoPropertyId = sensorId.getDesigoPropertyId();
                String lastUpdatedAtString = lastFailedAt.toString();
                Map<String, Object> properties = Map.of(
                        "DigitalTwinSensorId", id,
                        "DesigoId", desigoObjectId,
                        "DesigoPropertyId", desigoPropertyId,
                        "LastUpdatedAt", lastUpdatedAtString
                );
                lastUpdatedClient.updateRow(partitionKey, rowKey,properties);
            }
        }
        isHealthy = true;
    }

    @Override
    public boolean isHealthy() {
        return isHealthy;
    }

    @Override
    public List<String> getErrors() {
        return currentErrors;
    }

    @Override
    public String toString() {
        return "AzureTrendsLastUpdatedService{" +
                "config=" + config +
                ", repository=" + repository +
                ", lastUpdatedClient=" + lastUpdatedClient +
                ", lastFailedClient=" + lastFailedClient +
                '}';
    }
}
