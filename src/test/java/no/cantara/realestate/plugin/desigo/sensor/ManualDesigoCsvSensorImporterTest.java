package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.config.ApplicationProperties;
import no.cantara.realestate.sensors.SensorId;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;


class ManualDesigoCsvSensorImporterTest {
    private static final Logger log = getLogger(ManualDesigoCsvSensorImporterTest.class);

    /*
    # CSV SensorMappings
    sensormappings.csv.enabled=false
    sensormappings.csv.directory=sensor-mappings
    sensormappings.csv.filePrefix=Desigo
     */
    public static void main(String[] args) {
        ApplicationProperties coinfig = ApplicationProperties.builder().defaults().buildAndSetStaticSingleton();
        String importDirectoryPath = coinfig.get("sensormappings.csv.directory");
        File importDirectory = new File(importDirectoryPath);
        DesigoCsvSensorImporter csvImporter = new DesigoCsvSensorImporter(importDirectory);
        String filePrefix = coinfig.get("sensormappings.csv.filePrefix");
        List<SensorId> sensorIds = csvImporter.importSensors(filePrefix);
        for (SensorId sensorId : sensorIds) {
            System.out.println("sensorId = " + sensorId);
        }
        log.info("Imported {} sensors from {}/{}...csv files", sensorIds.size(), importDirectoryPath, filePrefix);
    }

}