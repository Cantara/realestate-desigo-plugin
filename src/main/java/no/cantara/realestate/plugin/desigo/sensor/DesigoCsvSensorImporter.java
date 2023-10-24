package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.csv.CsvCollection;
import no.cantara.realestate.csv.CsvReader;
import no.cantara.realestate.importer.CsvSensorImporter;
import no.cantara.realestate.semantics.rec.SensorRecObject;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoCsvSensorImporter extends CsvSensorImporter {
    private static final Logger log = getLogger(DesigoCsvSensorImporter.class);

    public DesigoCsvSensorImporter(File importDirectory) {
        super(importDirectory);
    }

    public List<SensorId> importSensorsFromFile(Path filepath) {
        List<SensorId> sensorIds = new ArrayList<>();
        CsvCollection collection = CsvReader.parse(filepath.toString());
        log.debug("ColumnNames: {}",collection.getColumnNames());
        for (Map<String, String> record : collection.getRecords()) {
            //MetasysObjectReference,MetasysObjectId
            SensorId sensorId = new DesigoSensorId( record.get("DesigoId"),record.get("DesigoPropertyId"));
            sensorIds.add(sensorId);
        }
        return sensorIds;
    }

    @Override
    public List<MappedSensorId> importMappedIdFromFile(Path filepath) {
        List<MappedSensorId> mappedSensorIds = new ArrayList<>();
        CsvCollection collection = CsvReader.parse(filepath.toString());
        List<String> columnNames = collection.getColumnNames();
        log.debug("ColumnNames: {}", columnNames);
        for (Map<String, String> record : collection.getRecords()) {

            SensorId sensorId = new DesigoSensorId( record.get("DesigoId"),record.get("DesigoPropertyId"));
            SensorRecObject sensorRecObject = importSensorRecObject(columnNames, record);
            MappedSensorId mappedSensorId = new MappedSensorId(sensorId, sensorRecObject);
            mappedSensorIds.add(mappedSensorId);
        }
        return mappedSensorIds;
    }
}
