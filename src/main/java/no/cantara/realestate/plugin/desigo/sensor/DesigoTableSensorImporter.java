package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.importer.SensorImporter;
import no.cantara.realestate.semantics.rec.SensorRecObject;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.cantara.realestate.importer.SensorMapper.importSensorRecObject;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoTableSensorImporter implements SensorImporter {
    private static final Logger log = getLogger(DesigoTableSensorImporter.class);

    private final List<Map<String, String>> tableRows;

    public DesigoTableSensorImporter(List<Map<String, String>> tableRows) {
        this.tableRows = tableRows;
    }


    @Override
    public List<SensorId> importSensors(String sourceType) {
        List<SensorId> sensorIds = new ArrayList<>();
        for (Map<String, String> row : tableRows) {
            SensorId sensorId = new DesigoSensorId( row.get("DesigoId"), row.get("DesigoPropertyId"));
            if (row.containsKey("DesigoTrendId")) {
                ((DesigoSensorId) sensorId).setTrendId(row.get("DesigoTrendId"));
            }
            if (row.containsKey("DigitalTwinSensorId")) {
                sensorId.setId(row.get("DigitalTwinSensorId"));
            } else if (row.containsKey("RecId")) {
                sensorId.setId(row.get("RecId"));
            }
            sensorIds.add(sensorId);
        }
        return sensorIds;

    }

    @Override
    public List<MappedSensorId> importMappedId(String sourceType) {

        List<MappedSensorId> mappedSensorIds = new ArrayList<>();
        for (Map<String, String> row : tableRows) {
            List<String> columnNames = new ArrayList<>(row.keySet());
            SensorId sensorId = new DesigoSensorId(row.get("DesigoId"), row.get("DesigoPropertyId"));
            if (row.containsKey("DesigoTrendId")) {
                ((DesigoSensorId) sensorId).setTrendId(row.get("DesigoTrendId"));
            }
            SensorRecObject sensorRecObject = importSensorRecObject(columnNames, row);
            MappedSensorId mappedSensorId = new MappedSensorId(sensorId, sensorRecObject);
            mappedSensorIds.add(mappedSensorId);
        }
        return mappedSensorIds;
    }
}
