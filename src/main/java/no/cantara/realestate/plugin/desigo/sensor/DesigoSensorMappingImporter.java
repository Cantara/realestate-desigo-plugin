package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.importer.CsvSensorImporter;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;
import no.cantara.realestate.sensors.MappedSensorId;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoSensorMappingImporter extends PluginSensorMappingImporter {
    private static final Logger log = getLogger(DesigoSensorMappingImporter.class);
    private long importedSensorMappingsCount;

    public DesigoSensorMappingImporter(PluginConfig config) {
        super(config);
    }


    @Override
    public List<MappedSensorId> importSensorMappings() {
        long totalCount = 0;
        List<MappedSensorId> mappedSensorIds = new ArrayList<>();
        boolean importFromAzureTable = config.asBoolean("sensormappings.azure.enabled", false);
        if (importFromAzureTable) {
            String connectionString = (String) config.get("sensormappings.azure.connectionString");
            String tableName = config.asString("sensormappings.azure.tableName", null);
            List<MappedSensorId> azureTableSensorMappings = importAzureTableConfig(connectionString, tableName);
            log.info("Imported {} Desigo Sensor configs from Azure Table {}", azureTableSensorMappings.size(), tableName);
            totalCount += azureTableSensorMappings.size();
            mappedSensorIds.addAll(azureTableSensorMappings);
        }
        boolean importFromCsv = config.asBoolean("sensormappings.csv.enabled", false);
        if (importFromCsv) {
            String configDirectory = config.asString("sensormappings.csv.directory", null);
            String filePrefix = config.asString("sensormappings.csv.filePrefix", null);
            List<MappedSensorId> csvSensorMappings =  importCsvConfig(configDirectory,filePrefix);
            log.info("Imported {} Desigo Sensor configs from directory {}", csvSensorMappings.size(), configDirectory);
            totalCount += csvSensorMappings.size();
            mappedSensorIds.addAll(csvSensorMappings);
        }
        log.info("Imported {} Desigo Sensor configs in total", totalCount);
        this.importedSensorMappingsCount = totalCount;
        return mappedSensorIds;

    }

    public List<MappedSensorId> importAzureTableConfig(String connectionString, String tableName) {
        AzureTableClient tableClient = new AzureTableClient(connectionString, tableName);
        List<Map<String, String>> rows = tableClient.listRows("1");
        List<MappedSensorId> mappedSensorIds = new DesigoTableSensorImporter(rows).importMappedId("Desigo");
        return mappedSensorIds;
    }


    public List<MappedSensorId> importCsvConfig(String configDirectory, String filePrefix) {
        File importDirectory = new File(configDirectory);
        CsvSensorImporter csvImporter = new DesigoCsvSensorImporter(importDirectory);
        List<MappedSensorId> mappedSensorIds = csvImporter.importMappedId(filePrefix);
        log.info("Imported {} Metasys Sensor configs from directory {}", mappedSensorIds.size(), importDirectory);
        return mappedSensorIds;
    }

    public long getImportedSensorMappingsCount() {
        return importedSensorMappingsCount;
    }
}
