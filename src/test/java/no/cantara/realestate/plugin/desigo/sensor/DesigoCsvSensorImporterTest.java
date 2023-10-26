package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory.PLUGIN_ID;
import static org.junit.jupiter.api.Assertions.*;

class DesigoCsvSensorImporterTest {

    private DesigoCsvSensorImporter importer;
    private ApplicationProperties config;

    @BeforeAll
    static void beforeAll() {
        ApplicationProperties.builder().classpathPropertiesFile("desigoImporter.properties").buildAndSetStaticSingleton();
    }

    @BeforeEach
    void setUp() {
        config = ApplicationProperties.getInstance();
        assertNotNull(config.get("sensormappings.csv.directory"));
        assertTrue(config.asBoolean("sensormappings.csv.enabled", false), "sensormappings.csv.enabled should be true");
        assertEquals(config.get("sensormappings.csv.filePrefix"), PLUGIN_ID);
        String csvDirectory = config.get("sensormappings.csv.directory");
        importer = new DesigoCsvSensorImporter(new File(csvDirectory));
    }

    @Test
    void importSensors() {
        List<SensorId> sensors = importer.importSensors(PLUGIN_ID);
        assertNotNull(sensors);
        assertEquals(1, sensors.size());
        DesigoSensorId sensorId = (DesigoSensorId) sensors.get(0);
        assertEquals("System1:GmsDevice_2_1414052_121634835", sensorId.getDesigoId());
        assertEquals("RAQual_Present_Value", sensorId.getDesigoPropertyId());
        assertEquals("System1:GmsDevice_2_1212052_83886086.general.Data:_offline.._value", sensorId.getTrendId());
        assertEquals("System1:GmsDevice_2_1414052_121634835.RAQual_Present_Value", sensorId.getMappingKey().getKey());
    }

    @Test
    void importMappedId() {
        List<MappedSensorId> mappedIds = importer.importMappedId(PLUGIN_ID);
        assertNotNull(mappedIds);
        assertEquals(1, mappedIds.size());
        MappedSensorId mappedId = mappedIds.get(0);
        DesigoSensorId sensorId = (DesigoSensorId) mappedId.getSensorId();
        assertEquals("System1:GmsDevice_2_1414052_121634835", sensorId.getDesigoId());
        assertEquals("RAQual_Present_Value", sensorId.getDesigoPropertyId());
        assertEquals("System1:GmsDevice_2_1212052_83886086.general.Data:_offline.._value", sensorId.getTrendId());
        assertEquals("System1:GmsDevice_2_1414052_121634835.RAQual_Present_Value", sensorId.getMappingKey().getKey());
        assertEquals("433.012-OE101-Energy", mappedId.getRec().getTfm().getTfm());
        assertEquals("RealEstateCoreSensor-1", mappedId.getRec().getRecId());

    }
}