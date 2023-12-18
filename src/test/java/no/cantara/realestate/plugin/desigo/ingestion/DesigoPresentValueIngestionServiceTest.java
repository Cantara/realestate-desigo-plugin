package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoPresentValue;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DesigoPresentValueIngestionServiceTest {

    private DesigoPresentValueIngestionService ingestionService;
    private ObservationListener observationListener;
    private PluginConfig config;
    private NotificationListener notificationListener;
    private DesigoApiClientRest desigoApiClient;

    @BeforeEach
    void setUp() {
        this.observationListener = mock(ObservationListener.class);
        this.notificationListener = mock(NotificationListener.class);
        this.config = mock(PluginConfig.class);
        this.desigoApiClient = mock(DesigoApiClientRest.class);
        this.ingestionService = new DesigoPresentValueIngestionService( config, observationListener, notificationListener, desigoApiClient);
    }

    @Test
    void ingestPresentValues() throws URISyntaxException {
        DesigoPresentValue presentValue = new DesigoPresentValue();
        presentValue.setValue(1234L);
        presentValue.setSensorId("sensor1");
        DesigoSensorId sensorId = new DesigoSensorId("desigoId1", "propertyId2");
        ingestionService.addSubscription(sensorId);
        when(desigoApiClient.findPresentValue(any())).thenReturn(presentValue);
        ingestionService.ingestPresentValues();
        verify(observationListener, times(1)).observedValue(any());
    }
}