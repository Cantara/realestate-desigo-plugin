package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;
import no.cantara.realestate.sensors.MappedSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DesigoRealEstatePluginFactoryTest {

    private static PluginConfig config;
    private DesigoRealEstatePluginFactory applicationFactory;


    @BeforeEach
    void setUp() {
        Properties properties = new Properties();
        properties.put("sensormappings.simulator.enabled", "true");
        config = new PluginConfig(properties);
        applicationFactory = new DesigoRealEstatePluginFactory();
        applicationFactory.initialize(config);
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
}