package no.cantara.realestate.plugin.desigo;

import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoPresentValueIngestionServiceSimulator;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionServiceSimulator;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingImporter;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingSimulator;
import no.cantara.realestate.plugins.RealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;

import java.util.ArrayList;
import java.util.List;

public class DesigoRealEstatePluginFactory  implements RealEstatePluginFactory {
    private PluginConfig config = null;
    public static String PLUGIN_ID = "Desigo";

    @Override
    public String getId() {
        return PLUGIN_ID;
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
        List<IngestionService> ingestionServices = new ArrayList<>();
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        boolean useSimulators = config.asBoolean("sensormappings.simulator.enabled", false);
        if (useSimulators) {
            ingestionServices.add(new DesigoPresentValueIngestionServiceSimulator());
            ingestionServices.add(new DesigoTrendsIngestionServiceSimulator());
        }
        return ingestionServices;
    }

    @Override
    public List<DistributionService> createDistributionServices() {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        return null;
    }
}
