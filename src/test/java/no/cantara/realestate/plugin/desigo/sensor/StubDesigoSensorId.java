package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StubDesigoSensorId {

    public static DesigoSensorId createDesigoSensorStub() {
        DesigoSensorId desigoSensorId =  new DesigoSensorId(null,"desigoId1", "propertyId1");
        desigoSensorId.setId("SensorId1");
        desigoSensorId.setTrendId("trend1");
        return desigoSensorId;
    }

    @Test
    void testEquals() {
        DesigoSensorId desigoSensorId =  createDesigoSensorStub();
        DesigoSensorId desigoSensorIdSame = createDesigoSensorStub();
        assertTrue(desigoSensorId.equals(desigoSensorIdSame));
        assertEquals(desigoSensorId.hashCode(), desigoSensorIdSame.hashCode());

    }
}
