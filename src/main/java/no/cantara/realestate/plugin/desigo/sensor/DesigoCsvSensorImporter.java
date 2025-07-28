package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.csv.CsvCollection;
import no.cantara.realestate.csv.CsvReader;
import no.cantara.realestate.importer.CsvSensorImporter;
import no.cantara.realestate.rec.SensorRecObject;
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
            DesigoSensorId sensorId = new DesigoSensorId( null, record.get("DesigoId"),record.get("DesigoPropertyId"));
            if (record.containsKey("DesigoTrendId")) {
                sensorId.setTrendId(record.get("DesigoTrendId"));
            }
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

            DesigoSensorId sensorId = new DesigoSensorId(null, record.get("DesigoId"),record.get("DesigoPropertyId"));
            if(record.containsKey("DesigoTrendId")) {
                sensorId.setTrendId(record.get("DesigoTrendId"));
            }
            SensorRecObject sensorRecObject = importSensorRecObject(columnNames, record);
            MappedSensorId mappedSensorId = new MappedSensorId(sensorId, sensorRecObject);
            mappedSensorIds.add(mappedSensorId);
        }
        return mappedSensorIds;
    }
}
