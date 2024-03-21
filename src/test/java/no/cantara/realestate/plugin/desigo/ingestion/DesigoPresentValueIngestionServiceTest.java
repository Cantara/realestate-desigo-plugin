package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoPresentValue;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URISyntaxException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DesigoPresentValueIngestionServiceTest {

    private DesigoPresentValueIngestionService ingestionService;
    private ObservationListener observationListener;
    private PluginConfig config;
    private NotificationListener notificationListener;
    private DesigoApiClientRest desigoApiClient;
    private DesigoSensorId sensorId;
    private DesigoPresentValue presentValue;

    @BeforeEach
    void setUp() {
        this.observationListener = mock(ObservationListener.class);
        this.notificationListener = mock(NotificationListener.class);
        this.config = mock(PluginConfig.class);
        this.desigoApiClient = mock(DesigoApiClientRest.class);
        this.ingestionService = new DesigoPresentValueIngestionService( config, observationListener, notificationListener, desigoApiClient);
        sensorId = new DesigoSensorId("desigoId1", "propertyId2");
        ingestionService.addSubscription(sensorId);
        presentValue = new DesigoPresentValue();
        presentValue.setValue(1234L);
        presentValue.setSensorId("sensor1");
    }

    @Test
    void ingestPresentValues() throws URISyntaxException {
        when(desigoApiClient.findPresentValue(any())).thenReturn(presentValue);
        ingestionService.ingestPresentValues();
        verify(observationListener, times(1)).observedValue(any());
    }

    @Test
    void ingestPresentValuesIsNull() throws URISyntaxException {
        presentValue.setReliable(null);
        when(desigoApiClient.findPresentValue(any())).thenReturn(presentValue);
        ingestionService.ingestPresentValues();
        verify(observationListener, times(1)).observedValue(any());
    }
    @Test
    void ingestPresentValuesIsReliable() throws URISyntaxException {
        when(desigoApiClient.findPresentValue(any())).thenReturn(presentValue);
        ingestionService.ingestPresentValues();
        verify(observationListener, times(1)).observedValue(any());
    }

    @Test
    void ingestPresentValueIsMissingObservedAt() throws URISyntaxException {
        ArgumentCaptor<ObservedValue> observedValueCaptor = ArgumentCaptor.forClass(ObservedValue.class);
        Instant observedAt = null;
        presentValue.setObservedAt(observedAt);
        when(desigoApiClient.findPresentValue(any())).thenReturn(presentValue);
        ingestionService.ingestPresentValues();

        verify(observationListener, times(1)).observedValue(observedValueCaptor.capture());
        assertTrue(observedValueCaptor.getValue().getObservedAt() != null);
    }
}