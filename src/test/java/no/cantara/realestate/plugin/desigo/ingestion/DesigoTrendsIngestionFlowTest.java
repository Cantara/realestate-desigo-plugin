package no.cantara.realestate.plugin.desigo.ingestion;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.testsupport.ApplicationPropertiesTestHelper;
import no.cantara.realestate.observations.ConfigMessage;
import no.cantara.realestate.observations.ConfigValue;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.observations.ObservedValue;
import no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory;
import no.cantara.realestate.plugin.desigo.automationserver.SdClientSimulator;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingSimulator;
import no.cantara.realestate.plugins.RealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.sensors.SensorId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory.PLUGIN_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DesigoTrendsIngestionFlowTest {

    private ApplicationProperties config;
    private DesigoSensorMappingSimulator sensorMappingSimulator;
    private PluginConfig desigoConfig;
    private ObservationListener observationListener;
    private NotificationListener notificationListener;

    private List<ObservedValue> observedValues;

    @BeforeAll
    static void beforeAll() {
        ApplicationPropertiesTestHelper.enableMutableSingleton();

    }

    @BeforeEach
    void setUp() {
        observedValues = new ArrayList<>();
        observationListener = createObservationListenerStub(observedValues);
        notificationListener = mock(NotificationListener.class);
        config = ApplicationProperties.builder().classpathPropertiesFile("desigoTrendFlowSimulators.properties").build();
        assertTrue(config.asBoolean("Desigo.ingestionServices.simulator.enabled", true), "ingestionServices.simulator.enabled should be false");
        assertTrue(config.asBoolean("Desigo.sdclient.simulator.enabled", false), "sdclient.simulator.enabled should be true");
        assertTrue(config.asBoolean("Desigo.sensormappings.simulator.enabled", false), "sensormappings.simulator.enabled should be true");
        desigoConfig = PluginConfig.fromMap(config.subMap(PLUGIN_ID));
        sensorMappingSimulator = new DesigoSensorMappingSimulator(desigoConfig);
    }




    @Test
    void testTheFlow() {
        //1. Create a Factory
        //2. Create DesigoTrendsIngestionService
        //3. Initialize TrendsService
        //4. Add Subscriptions
        //5. Open Connection
        //6. Read observations from simulator
        //7. Verify TrendsListener is updated
        RealEstatePluginFactory desigoFactory = new DesigoRealEstatePluginFactory();

        desigoFactory.initialize(desigoConfig);
        List<IngestionService> ingestionServices = desigoFactory.createIngestionServices(observationListener, notificationListener);
        DesigoTrendsIngestionService trendsService = null;
        for (IngestionService ingestionService : ingestionServices) {
            if (ingestionService instanceof DesigoTrendsIngestionService) {
                trendsService = (DesigoTrendsIngestionService) ingestionService;
            }
        }
        assertNotNull(trendsService);
        //3. Intialize TrendsService
        trendsService.initialize(desigoConfig);
        //4. Add Subscriptions
        SensorId sensorId = sensorMappingSimulator.importSensorMappings().get(0).getSensorId();
        trendsService.addSubscription(sensorId);
        //5. Open Connection
        trendsService.openConnection(observationListener, notificationListener);
        //6. Read observations from simulator
        //Simulate incomming sensor readings
        ((SdClientSimulator)trendsService.getDesigoApiClientRest()).simulateSensorReadings();
        //Trigger timed ingestion
        trendsService.ingestTrends();
        //7. Verify TrendsListener is updated
        assertEquals(1, observedValues.size());

    }

    private ObservationListener createObservationListenerStub(List<ObservedValue> observedValues) {
        ObservationListener listener = new ObservationListener() {
            @Override
            public void observedValue(ObservedValue observedValue) {
                observedValues.add(observedValue);
            }

            @Override
            public void observedConfigValue(ConfigValue configValue) {

            }

            @Override
            public void observedConfigMessage(ConfigMessage configMessage) {

            }

            @Override
            public Instant getWhenLastMessageObserved() {
                return Instant.ofEpochMilli(System.currentTimeMillis());
            }
        };
        return listener;
    }
}
