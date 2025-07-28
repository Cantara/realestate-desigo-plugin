package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;
import no.cantara.realestate.rec.SensorRecObject;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.SensorType;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import no.cantara.realestate.sensors.tfm.Tfm;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoSensorMappingSimulator extends PluginSensorMappingImporter {
    private static final Logger log = getLogger(DesigoSensorMappingSimulator.class);

    public DesigoSensorMappingSimulator(PluginConfig config) {
        super(config);
    }

    @Override
    public List<MappedSensorId> importSensorMappings() {
        log.warn("Using simulated SensorId's");
        List<MappedSensorId> mappedSensorIds = new ArrayList<>();
        SensorId simulatedCo2Sensor = new DesigoSensorId(null,"desigoId1", "desigoPropertyId1");
        ((DesigoSensorId) simulatedCo2Sensor).setTrendId("System1:GmsDevice_2_1212052_83886086.general.DataCo2:_offline.._value");
        MappedSensorId mappedSimulatedCo2Sensor = new MappedSensorId(simulatedCo2Sensor, buildRecStub("room1", SensorType.co2));
        SensorId simulatedTempSensor = new DesigoSensorId(null,"desigoId2", "desigoPropertyId2");
        ((DesigoSensorId) simulatedTempSensor).setTrendId("System1:GmsDevice_2_1212052_83886086.general.DataTemp:_offline.._value");
        MappedSensorId mappedSimulatedTempSensor = new MappedSensorId(simulatedTempSensor, buildRecStub("room1", SensorType.temp));
        mappedSensorIds.add(mappedSimulatedCo2Sensor);
        mappedSensorIds.add(mappedSimulatedTempSensor);
        return mappedSensorIds;
    }
    public static List<MappedSensorId> getSimulatedSensors() {
        DesigoSensorMappingSimulator simulator = new DesigoSensorMappingSimulator(null);
        return simulator.importSensorMappings();
    }
    public static SensorRecObject buildRecStub(String roomName, SensorType sensorType) {
        SensorRecObject recObject = new SensorRecObject(UUID.randomUUID().toString());
        recObject.setTfm(new Tfm(roomName + "-" + sensorType.name()));
        recObject.setRealEstate("TestRealEstate");
        recObject.setBuilding("TestBuilding");
        recObject.setFloor("1");
        recObject.setServesRoom(roomName);
        recObject.setPlacementRoom(roomName);
        recObject.setSensorType(sensorType.name());
        return recObject;
    }
}
