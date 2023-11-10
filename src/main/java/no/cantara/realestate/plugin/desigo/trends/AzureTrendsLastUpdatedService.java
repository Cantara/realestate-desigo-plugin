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

    private static final String PARTITION_KEY = "Desigo";


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
                log.trace("Could not parse lastUpdatedAtString: {} for RowKey {}. objectId.propertyId {}.{}.", lastUpdatedAtString, trendId, desigoObjectId, desigoPropertyId, e);
            }
        }
        DesigoSensorId sensorId = new DesigoSensorId(desigoObjectId, desigoPropertyId);
        sensorId.setId(id);
        sensorId.setTrendId(trendId);
        if (lastUpdatedAt != null) {
            repository.addLastUpdated(sensorId, lastUpdatedAt);
        } else {
            log.trace("LastUpdatedAt is null. Will not be added to LastUpdatedRepository. RowKey {}. objectId.propertyId {}.{}. ", trendId, desigoObjectId, desigoPropertyId);
        }
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
        if (connectionString == null) {
            log.warn("Missing configuration for Desigo.{}", AzureStorageTablesClient.CONNECTIONSTRING_KEY );
        }
        String lastUpdatedTableName = config.asString("trends.lastUpdated.tableName", null);
        if (lastUpdatedTableName == null) {
            log.warn("Missing configuration for Desigo.trends.lastUpdated.tableName");
        }
        AzureTableClient lastUpdatedClient = new AzureTableClient(connectionString, lastUpdatedTableName);
        log.info("Initialized lastUpdatedClient {} with lastUpdatedTableName {}", lastUpdatedClient, lastUpdatedTableName);
        return lastUpdatedClient;
    }
    public static AzureTableClient createLastFailedTableClient(PluginConfig config) {
        String connectionString = config.asString(AzureStorageTablesClient.CONNECTIONSTRING_KEY, null);
        if (connectionString == null) {
            log.warn("Missing configuration for Desigo.{}", AzureStorageTablesClient.CONNECTIONSTRING_KEY );
        }
        String lastFailedTableName = config.asString("trends.lastFailed.tableName", null);
        if (lastFailedTableName == null) {
            log.warn("Missing configuration for Desigo.trends.lastFailed.tableName");
        }
        AzureTableClient lastFailedClient = new AzureTableClient(connectionString, lastFailedTableName);
        log.info("Initialized lastFailedClient {} with lastFailedTableName {}", lastFailedClient, lastFailedTableName);
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
        int count = 0;
        int notUpdated = 0;
        String partitionKey = config.asString("trends.lastupdated.partitionKey", PARTITION_KEY);
        log.trace("trendsLastUpdated:Persist last updated to AzureTableClient for {} sensorIds with PartitionKey: {} ", sensorIds.size(), partitionKey);

        try {
            for (DesigoSensorId sensorId : sensorIds) {
                log.trace("trendsLastUpdated:Find LastUpdated in repository for sensorId: {}", sensorId);
                log.trace("trendsLastUpdated:SensorId: {}", sensorId.toString());
                log.trace("trendsLastUpdated:Repository: {}", repository.toString());
                repository.getTrendsLastUpdated().forEach((k, v) -> log.trace("trendsLastUpdated:Repository:Key: {}, Value: {}", k, v));
                log.trace("trendsLastUpdated:Repository:LastUpdated: {}", repository.getTrendsLastUpdated());
                Instant lastUpdatedAt = repository.getTrendsLastUpdated().get(sensorId);
                log.trace("trendsLastUpdated:Found LastUpdated {} in repository for sensorId: {}", lastUpdatedAt, sensorId);
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
                    log.trace("trendsLastUpdated:Persisting rowKey {} with lastUpdatedAt {}", rowKey, lastUpdatedAt, properties);
                    lastUpdatedClient.updateRow(partitionKey, rowKey, properties);
                    count++;
                } else {
                    log.trace("trendsLastUpdated:Not persisting rowKey {} with lastUpdatedAt {} for sensorId: {}", sensorId.getTrendId(), lastUpdatedAt, sensorId);
                    notUpdated++;
                }

            }
        } catch (Exception e) {
            log.error("trendsLastUpdated:Persisting {} sensorIds to AzureTable. Missing LastUpdatedAt count: {}. Exception: {}", count, notUpdated, e.getMessage(), e);
            throw e;
        }
        log.trace("trendsLastUpdated:Persisted {} sensorIds to AzureTable. Missing LastUpdatedAt count: {}", count, notUpdated);
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
