package no.cantara.realestate.plugin.desigo;

import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoPresentValueIngestionService;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoPresentValueIngestionServiceSimulator;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionService;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionServiceSimulator;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingImporter;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingSimulator;
import no.cantara.realestate.plugins.RealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;
import no.cantara.realestate.plugins.ingestion.TrendsIngestionService;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class DesigoRealEstatePluginFactory  implements RealEstatePluginFactory {
    private static final Logger log = getLogger(DesigoRealEstatePluginFactory.class);
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
        boolean useProdBasClient = config.asBoolean("sd.api.prod", false);
        if (useProdBasClient) {
            PresentValueIngestionService presentValueService = createPresentValueIngestionService();
            ingestionServices.add(presentValueService);
            TrendsIngestionService trendsIngestionService = createTrendsIngestionService();
            ingestionServices.add(trendsIngestionService);
        }
        return ingestionServices;
    }

    protected PresentValueIngestionService createPresentValueIngestionService() {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        PresentValueIngestionService presentValueService = new DesigoPresentValueIngestionService();
        return presentValueService;
    }
    protected TrendsIngestionService createTrendsIngestionService() {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        TrendsIngestionService trendsIngestionService = new DesigoTrendsIngestionService();
        return trendsIngestionService;
    }

    @Override
    public List<DistributionService> createDistributionServices() {
        log.warn("DistributionServices not implemented for Desigo");
        return null;
    }
}
