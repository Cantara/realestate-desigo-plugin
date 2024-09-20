package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.azure.storage.AzureStorageTablesClient;
import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.TrendSample;
import no.cantara.realestate.observations.Value;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoTrendSample;
import no.cantara.realestate.plugin.desigo.trends.AzureTrendsLastUpdatedService;
import no.cantara.realestate.plugin.desigo.trends.TrendsLastUpdatedRepository;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static no.cantara.realestate.plugin.desigo.sensor.StubDesigoSensorId.createDesigoSensorStub;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesigoTrendsIngestionServiceTest {

    private DesigoTrendsIngestionService trendsIngestionService;
    private PluginConfig config;
    private ObservationListener observationListener;
    private NotificationListener notificationListener;
    private DesigoApiClientRest desigoApiClient;
    private TrendsLastUpdatedRepository lastUpdatedRepository;
    private AzureTableClient lastUpdatedClient;
    private AzureTableClient lastFailedClient;
    private AzureTrendsLastUpdatedService azureTrendsLastUpdatedService;

    @BeforeEach
    void setUp() {
        lastUpdatedRepository = new TrendsLastUpdatedRepository();
        observationListener = mock(ObservationListener.class);
        notificationListener = mock(NotificationListener.class);
        desigoApiClient = mock(DesigoApiClientRest.class);
        lastUpdatedClient = mock(AzureTableClient.class);
        lastFailedClient = mock(AzureTableClient.class);
        Properties properties = new Properties();
        properties.setProperty("sd.api.url", "http://localhost:8080");
        properties.setProperty("sd.api.username", "username");
        properties.setProperty("sd.api.password", "password");
        properties.setProperty(AzureStorageTablesClient.CONNECTIONSTRING_KEY, "connectionstring");
        config = new PluginConfig(properties);
        azureTrendsLastUpdatedService = new AzureTrendsLastUpdatedService(config, lastUpdatedRepository, lastUpdatedClient, lastFailedClient);
        trendsIngestionService = new DesigoTrendsIngestionService(config, observationListener, notificationListener,desigoApiClient,azureTrendsLastUpdatedService);
    }

    @Test
    void trendsIngestionFlow() throws URISyntaxException {
//        Ensure TrendsIngestionService is intialized
        DesigoSensorId sensorId = createDesigoSensorStub();
//        lastUpdatedRepository.addLastUpdated(sensorId, Instant.now());
        azureTrendsLastUpdatedService.setLastUpdatedAt(sensorId, Instant.now().minusSeconds(50));
        when(desigoApiClient.findTrendSamplesByDate(any(), anyInt(), anyInt(), any(Instant.class))).thenReturn(buildDesigoTrendSamplesStub(sensorId));
        assertNotNull(trendsIngestionService.getConfig());
        assertEquals(4, trendsIngestionService.getConfig().size());
        trendsIngestionService.initialize(config);
        assertTrue(trendsIngestionService.isInitialized());
        assertNotNull(trendsIngestionService.getDesigoApiClientRest());
        //Open Connection
        trendsIngestionService.openConnection(observationListener, notificationListener);
        //Add subscriptions
        trendsIngestionService.addSubscription(sensorId);
        assertEquals(1, trendsIngestionService.getSubscriptionsCount());
        //Ingest trends
        trendsIngestionService.ingestTrends();
        assertEquals(1, trendsIngestionService.getNumberOfMessagesImported());
        assertEquals(1, lastUpdatedRepository.countLastUpdatedSensors());
        verify(observationListener, times(1)).observedValue(any());
//        verify(notificationListener, times(0)).
        //persist last updated
//        lastUpdatedRepository.getTrendsLastUpdated().entrySet().forEach(entry -> {
//            assertEquals(sensorId, entry.getKey());
//            assertNotNull(entry.getValue());
//        });
        verify(lastUpdatedClient, times(1)).updateRow(any(), any(), any());
        verify(lastFailedClient, times(0)).updateRow(any(), any(), any());
    }

    /*
    @Test
    void trendsLastUpdatedServiceIsNull() {
        trendsIngestionService = new DesigoTrendsIngestionService(config, observationListener, notificationListener,desigoApiClient,null);
        Exception exception = assertThrows(DesigoCloudConnectorException.class, () -> trendsIngestionService.ingestTrends());
        assertTrue( exception.getMessage().contains("TrendsLastUpdatedService is null") );
        assertTrue(exception.getMessage().contains("MessageId"));
        assertFalse(trendsIngestionService.isHealthy());
    }

     */

    private Set<TrendSample> buildDesigoTrendSamplesStub(DesigoSensorId sensorId) {
        Set<TrendSample> trendSamples = new HashSet<>();
        trendSamples.add(stubTrendSample(sensorId));
        return  trendSamples;
    }

    private DesigoTrendSample stubTrendSample(DesigoSensorId sensorId) {
        DesigoTrendSample trendSample = new DesigoTrendSample();
        trendSample.setTrendId(sensorId.getTrendId());
        trendSample.setObservedAt(Instant.now());
        trendSample.setObjectId(sensorId.getDesigoId());
        trendSample.setPropertyId(sensorId.getDesigoPropertyId());
        Value value = new Value();
        value.setValue(101);
        trendSample.setValue(101);
        return trendSample;
    }

}