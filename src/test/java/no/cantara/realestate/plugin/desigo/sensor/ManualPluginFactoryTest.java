package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.sensors.MappedSensorId;
import org.slf4j.Logger;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class ManualPluginFactoryTest {
    private static final Logger log = getLogger(ManualPluginFactoryTest.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("sensormappings.csv.enabled", "true");
        properties.setProperty("sensormappings.csv.directory", "src/test/resources/sensor-mappings");
        properties.setProperty("sensormappings.csv.filePrefix", "Desigo");
        PluginConfig pluginConfig = new PluginConfig(properties);
        DesigoRealEstatePluginFactory factory = new DesigoRealEstatePluginFactory();
        factory.initialize(pluginConfig);
        List<MappedSensorId> mappedSensorIds = factory.createSensorMappingImporter().importSensorMappings();
        assertEquals(1, mappedSensorIds.size());

    }
}
