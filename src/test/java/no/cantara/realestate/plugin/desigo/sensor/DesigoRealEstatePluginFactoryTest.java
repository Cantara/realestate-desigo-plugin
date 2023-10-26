package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.testsupport.ApplicationPropertiesTestHelper;
import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;
import no.cantara.realestate.sensors.MappedSensorId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory.PLUGIN_ID;
import static org.junit.jupiter.api.Assertions.*;

class DesigoRealEstatePluginFactoryTest {

    private ApplicationProperties config;
    private DesigoRealEstatePluginFactory applicationFactory;

    @BeforeAll
    static void beforeAll() {
        ApplicationPropertiesTestHelper.enableMutableSingleton();

    }
    @BeforeEach
    void setUp() {
        config = ApplicationProperties.builder().classpathPropertiesFile("desigoFacorySimulators.properties").build();
        Map<String, String> sensormappings = config.subMap(PLUGIN_ID);
        assertNotNull(sensormappings);
        assertTrue(sensormappings.keySet().size() > 0);

        PluginConfig pluginConfig = PluginConfig.fromMap(sensormappings);
        applicationFactory = new DesigoRealEstatePluginFactory();
        applicationFactory.initialize(pluginConfig);
    }

    @Test
    void shouldThrowException() {
        applicationFactory = new DesigoRealEstatePluginFactory();
        assertThrows(RealEstateException.class, () -> applicationFactory.createSensorMappingImporter());
    }

    @Test
    void createSensorMapper() {
        PluginSensorMappingImporter sensorMappingImporter = applicationFactory.createSensorMappingImporter();
        assertNotNull(sensorMappingImporter);
        List<MappedSensorId> sensorMappings = sensorMappingImporter.importSensorMappings();
        assertNotNull(sensorMappings);
        assertEquals(2, sensorMappings.size());
    }

    @Test
    void createIngestionSimulators() {
        List<IngestionService> ingestionServices = applicationFactory.createIngestionServices();
        assertEquals(2, ingestionServices.size());
    }
}