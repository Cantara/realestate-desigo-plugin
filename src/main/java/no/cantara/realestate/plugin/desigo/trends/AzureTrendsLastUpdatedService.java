package no.cantara.realestate.plugin.desigo.trends;

import no.cantara.realestate.azure.storage.AzureStorageTablesClient;
import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class AzureTrendsLastUpdatedService implements TrendsLastUpdatedService{
    private static final Logger log = getLogger(AzureTrendsLastUpdatedService.class);
    private final PluginConfig config;
    private final TrendsLastUpdatedRepository repository;

    private AzureTableClient lastUpdatedClient;
    private AzureTableClient lastFailedClient;


    public AzureTrendsLastUpdatedService(PluginConfig config, TrendsLastUpdatedRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    /*
    Used for testing
     */
    protected AzureTrendsLastUpdatedService(PluginConfig config, TrendsLastUpdatedRepository repository, AzureTableClient lastUpdatedClient, AzureTableClient lastFailedClient) {
        this.config = config;
        this.repository = repository;
        this.lastUpdatedClient = lastUpdatedClient;
        this.lastFailedClient = lastFailedClient;
    }

    @Override
    public void readLastUpdated() {
        verifyOrInitializeAzureTableClients();
        String partitionKey = config.asString("trends.lastupdated.partitionKey", "Desigo");
        lastUpdatedClient.listRows(partitionKey).forEach(tableEntity -> {
            String trendId = tableEntity.get("RowKey");
            String id = tableEntity.get("DigitalTwinSensorId");
            String desigoObjectId = tableEntity.get("DesigoId");
            String desigoPropertyId = tableEntity.get("DesigoPropertyId");
            String lastUpdatedAtString = tableEntity.get("LastUpdatedAt");
            Instant lastUpdatedAt = null;
            if (lastUpdatedAtString != null && ! lastUpdatedAtString.isEmpty()) {
                try {
                    Instant.parse(lastUpdatedAtString);
                } catch (Exception e) {
                    log.warn("Could not parse lastUpdatedAtString: {}", lastUpdatedAtString, e);
                }
            }
            DesigoSensorId sensorId = new DesigoSensorId(desigoObjectId, desigoPropertyId);
            sensorId.setId(id);
            sensorId.setTrendId(trendId);
            repository.addLastUpdated(sensorId, lastUpdatedAt);
        });
    }

    void verifyOrInitializeAzureTableClients() {
        //trends.lastupdated.enabled=true
        //trends.lastupdated.tableName=lastupdated
        //trends.lastupdated.partitionKey=Desigo
        String connectionString = config.asString(AzureStorageTablesClient.CONNECTIONSTRING_KEY, null);
        if (lastUpdatedClient == null) {

            String tableName = config.asString("trends.lastupdated.tableName", null);
            lastUpdatedClient = new AzureTableClient(connectionString, tableName);
        }
        if (lastFailedClient == null) {
            String tableName = config.asString("trends.lastFailed.tableName", null);
            lastFailedClient = new AzureTableClient(connectionString, tableName);
        }
    }

    @Override
    public Instant getLastUpdatedAt(DesigoSensorId sensorId) {
        return null;
    }

    @Override
    public void setLastUpdatedAt(DesigoSensorId sensorId, Instant lastUpdatedAt) {
        repository.addLastUpdated(sensorId, lastUpdatedAt);
    }

    @Override
    public void setLastFailedAt(DesigoSensorId sensorId, Instant lastFailedAt) {
        repository.addLastFailed(sensorId, lastFailedAt);
    }

    @Override
    public void persistLastUpdated(List<DesigoSensorId> sensorIds) {
        String partitionKey = config.asString("trends.lastupdated.partitionKey", "Desigo");
        for (DesigoSensorId sensorId : sensorIds) {
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
    }
}
