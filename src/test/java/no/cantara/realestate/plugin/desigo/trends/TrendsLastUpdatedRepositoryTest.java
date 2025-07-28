package no.cantara.realestate.plugin.desigo.trends;

import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendsLastUpdatedRepositoryTest {

    private TrendsLastUpdatedRepository trendsLastUpdatedRepository;

    @BeforeEach
    void setUp() {
        trendsLastUpdatedRepository = new TrendsLastUpdatedRepository();
    }

    @Test
    void getTrendsLastUpdatedTest() {
        DesigoSensorId sensorId = new DesigoSensorId(null,"System1:GmsDevice_2_101414_121634835", "RAQual_Present_Value");
        sensorId.setTrendId("System1:GmsDevice_2_101414_83886086.general.Data:_offline.._value");
        sensorId.setId("TODOSensor1");
        trendsLastUpdatedRepository.addLastUpdated(sensorId, Instant.parse("2023-11-10T08:05:57Z"));
        DesigoSensorId testTrendId = new DesigoSensorId(null,"Id1", "prop1");
        testTrendId.setTrendId("TestTrendTrend1");
        testTrendId.setId("Sensor-1234");
        trendsLastUpdatedRepository.addLastUpdated(testTrendId, Instant.parse("2023-11-10T08:05:57Z"));
        assertEquals(Instant.parse("2023-11-10T08:05:57Z"),trendsLastUpdatedRepository.getTrendsLastUpdated().get(sensorId));
    }
}