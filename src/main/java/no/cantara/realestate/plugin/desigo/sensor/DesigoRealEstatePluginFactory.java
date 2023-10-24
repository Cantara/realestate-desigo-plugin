package no.cantara.realestate.plugin.desigo.sensor;

import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.plugins.RealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;

import java.util.List;

public class DesigoRealEstatePluginFactory  implements RealEstatePluginFactory {
    private PluginConfig config = null;

    @Override
    public String getId() {
        return "Desigo";
    }

    @Override
    public String getDisplayName() {
        return "Siemens Desigo connector";
    }

    @Override
    public String getDescription() {
        return "Read PresentValue and Trends from Desigo CC";
    }

    @Override
    public void initialize(PluginConfig pluginConfig) {
        this.config = pluginConfig;
    }

    @Override
    public PluginSensorMappingImporter createSensorMappingImporter() {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        PluginSensorMappingImporter importer =  null;
        boolean useSensorMappingSimulator = config.asBoolean("sensormappings.simulator.enabled", false);
        if (useSensorMappingSimulator) {
            importer = new DesigoSensorMappingSimulator(config);
        } else {
            importer = new DesigoSensorMappingImporter(config);
        }
        return importer;
    }

    @Override
    public List<IngestionService> createIngestionServices() {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        return null;
    }

    @Override
    public List<DistributionService> createDistributionServices() {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        return null;
    }
}
