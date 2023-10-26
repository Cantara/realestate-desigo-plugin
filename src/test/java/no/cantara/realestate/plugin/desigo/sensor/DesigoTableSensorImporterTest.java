package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory.PLUGIN_ID;
import static org.junit.jupiter.api.Assertions.*;

class DesigoTableSensorImporterTest {

    private DesigoTableSensorImporter importer;
    private ApplicationProperties config;

    @BeforeAll
    static void beforeAll() {
        ApplicationProperties.builder().classpathPropertiesFile("desigoImporter.properties").buildAndSetStaticSingleton();
    }

    @BeforeEach
    void setUp() {
        config = ApplicationProperties.getInstance();
        assertNotNull(config.get("sensormappings.csv.directory"));
        assertTrue(config.asBoolean("sensormappings.table.enabled", false), "sensormappings.table.enabled should be true");

        List<Map<String, String>> tableRows = List.of(
                Map.of("DesigoId", "d1", "DesigoPropertyId", "p1", "DesigoTrendId", "t1", "Tfm", "tfm1", "RecId", "rec1"),
                Map.of("DesigoId", "2", "DesigoPropertyId", "2", "DesigoTrendId", "2"),
                Map.of("DesigoId", "3", "DesigoPropertyId", "3", "DesigoTrendId", "3")
        );
        importer = new DesigoTableSensorImporter(tableRows);
    }

    @Test
    void importSensors() {
        List<SensorId> sensors = importer.importSensors(PLUGIN_ID);
        assertNotNull(sensors);
        assertEquals(3, sensors.size());
        DesigoSensorId sensorId = (DesigoSensorId) sensors.get(0);
        assertEquals("d1", sensorId.getDesigoId());
        assertEquals("p1", sensorId.getDesigoPropertyId());
        assertEquals("t1", sensorId.getTrendId());
        assertEquals("d1.p1", sensorId.getMappingKey().getKey());
    }

    @Test
    void importMappedId() {
        List<MappedSensorId> mappedIds = importer.importMappedId(PLUGIN_ID);
        assertNotNull(mappedIds);
        assertEquals(3, mappedIds.size());
        MappedSensorId mappedId = mappedIds.get(0);
        DesigoSensorId sensorId = (DesigoSensorId) mappedId.getSensorId();
        assertEquals("d1", sensorId.getDesigoId());
        assertEquals("p1", sensorId.getDesigoPropertyId());
        assertEquals("t1", sensorId.getTrendId());
        assertEquals("d1.p1", sensorId.getMappingKey().getKey());
        assertEquals("tfm1", mappedId.getRec().getTfm().getTfm());
        assertEquals("rec1", mappedId.getRec().getRecId());

    }
}