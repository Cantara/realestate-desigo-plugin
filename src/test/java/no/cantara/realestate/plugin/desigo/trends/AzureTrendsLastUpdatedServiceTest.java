package no.cantara.realestate.plugin.desigo.trends;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

class AzureTrendsLastUpdatedServiceTest {

    private PluginConfig pluginConfig;
    private TrendsLastUpdatedRepository trendsLastUpdatedRepository;

    private AzureTableClient lastUpdatedClient;
    private AzureTableClient lastFailedClient;

    private TableClient tableClient;

    private AzureTrendsLastUpdatedService trendsLastUpdatedService;

    @BeforeEach
    void setUp() {
        Properties config = new Properties();
        config.put("trends.lastupdated.partitonKey", "Desigo");
        pluginConfig = new PluginConfig(config);
        trendsLastUpdatedRepository = new TrendsLastUpdatedRepository();
        tableClient = mock(TableClient.class);
        lastUpdatedClient = new AzureTableClient(tableClient);
        lastFailedClient = new AzureTableClient(tableClient);
        trendsLastUpdatedService = new AzureTrendsLastUpdatedService(pluginConfig, trendsLastUpdatedRepository, lastUpdatedClient, lastFailedClient);
    }

    @Test
    void readLastUpdated() {
        ArrayList<TableEntity> stubRows = new ArrayList<>();
        TableEntity tableEntitiy = new TableEntity("Desigo", "trend1");
        tableEntitiy.addProperty("DigitalTwinSensorId", "id1");
        tableEntitiy.addProperty("DesigoId", "des1");
        tableEntitiy.addProperty("DesigoPropertyId", "prop1");
        Instant newYearsEve = Instant.parse("2020-01-01T00:00:00.00Z");
        tableEntitiy.addProperty("LastUpdatedAt", newYearsEve.toString());
        stubRows.add(tableEntitiy);
        PagedIterable<TableEntity> pagedIterable = mock(PagedIterable.class);
        when(pagedIterable.stream()).thenReturn(stubRows.stream());
        when(tableClient.listEntities(isA(ListEntitiesOptions.class), isNull(), isNull())).thenReturn(pagedIterable);
        trendsLastUpdatedService.readLastUpdated();
        assertEquals(1, trendsLastUpdatedRepository.countLastUpdatedSensors());
//        verify(trendsLastUpdatedRepository).setLastUpdated(isA(DesigoSensorId.class), isA(Instant.class));
    }

    @Test
    void readLastUpdatedMissingProps() {
        ArrayList<TableEntity> stubRows = new ArrayList<>();
        TableEntity tableEntitiy = new TableEntity("Desigo", "trend1");
        Instant newYearsEve = Instant.parse("2020-01-01T00:00:00.00Z");
        tableEntitiy.addProperty("LastUpdatedAt", newYearsEve.toString());
        stubRows.add(tableEntitiy);
        PagedIterable<TableEntity> pagedIterable = mock(PagedIterable.class);
        when(pagedIterable.stream()).thenReturn(stubRows.stream());
        when(tableClient.listEntities(isA(ListEntitiesOptions.class), isNull(), isNull())).thenReturn(pagedIterable);
        trendsLastUpdatedService.readLastUpdated();
        assertEquals(1, trendsLastUpdatedRepository.countLastUpdatedSensors());
//        verify(trendsLastUpdatedRepository).setLastUpdated(isA(DesigoSensorId.class), isA(Instant.class));
    }

    @Test
    void persistLastUpdatedAt() {
        DesigoSensorId sensorId = new DesigoSensorId("desigoId", "desigoPropertyId");
        sensorId.setId("id");
        sensorId.setTrendId("trendId");
        Instant newYearsEve = Instant.parse("2020-01-01T00:00:00.00Z");
        trendsLastUpdatedRepository.addLastUpdated(sensorId, newYearsEve);
        List<DesigoSensorId> sensorIds = new ArrayList<>();
        sensorIds.add(sensorId);
        trendsLastUpdatedService.persistLastUpdated(sensorIds);
        assertEquals(1, trendsLastUpdatedRepository.countLastUpdatedSensors());
        verify(tableClient, times(1)).updateEntity(isA(TableEntity.class));
    }

    @Test
    void persistLastFailedAt() {
        DesigoSensorId sensorId = new DesigoSensorId("desigoId", "desigoPropertyId");
        sensorId.setId("id");
        sensorId.setTrendId("trendId");
        Instant newYearsEve = Instant.parse("2020-01-01T00:00:00.00Z");
        trendsLastUpdatedRepository.addLastFailed(sensorId, newYearsEve);
        List<DesigoSensorId> sensorIds = new ArrayList<>();
        sensorIds.add(sensorId);
        trendsLastUpdatedService.persistLastFailed(sensorIds);
        assertEquals(1, trendsLastUpdatedRepository.countLastFailedSensors());
        verify(tableClient, times(1)).updateEntity(isA(TableEntity.class));
    }
}