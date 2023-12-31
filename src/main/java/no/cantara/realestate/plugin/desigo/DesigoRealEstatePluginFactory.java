package no.cantara.realestate.plugin.desigo;

import no.cantara.realestate.RealEstateException;
import no.cantara.realestate.automationserver.BasClient;
import no.cantara.realestate.azure.storage.AzureTableClient;
import no.cantara.realestate.observations.ObservationListener;
import no.cantara.realestate.plugin.desigo.automationserver.DesigoApiClientRest;
import no.cantara.realestate.plugin.desigo.automationserver.SdClientSimulator;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoPresentValueIngestionService;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoPresentValueIngestionServiceSimulator;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionService;
import no.cantara.realestate.plugin.desigo.ingestion.DesigoTrendsIngestionServiceSimulator;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingImporter;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingSimulator;
import no.cantara.realestate.plugin.desigo.trends.AzureTrendsLastUpdatedService;
import no.cantara.realestate.plugin.desigo.trends.InMemoryTrendsLastUpdatedService;
import no.cantara.realestate.plugin.desigo.trends.TrendsLastUpdatedRepository;
import no.cantara.realestate.plugin.desigo.trends.TrendsLastUpdatedService;
import no.cantara.realestate.plugins.RealEstatePluginFactory;
import no.cantara.realestate.plugins.config.PluginConfig;
import no.cantara.realestate.plugins.distribution.DistributionService;
import no.cantara.realestate.plugins.ingestion.IngestionService;
import no.cantara.realestate.plugins.ingestion.PresentValueIngestionService;
import no.cantara.realestate.plugins.ingestion.TrendsIngestionService;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.plugins.sensormapping.PluginSensorMappingImporter;
import org.slf4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static no.cantara.realestate.plugin.desigo.trends.AzureTrendsLastUpdatedService.createLastFailedTableClient;
import static no.cantara.realestate.plugin.desigo.trends.AzureTrendsLastUpdatedService.createLastUpdatedTableClient;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoRealEstatePluginFactory  implements RealEstatePluginFactory {
    private static final Logger log = getLogger(DesigoRealEstatePluginFactory.class);
    private PluginConfig config = null;
    public static String PLUGIN_ID = "Desigo";
    private URI apiUri;
    private BasClient desigoApiClient;

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
        boolean useProdBasClient = config.asBoolean("sd.api.prod", false);
        boolean useBasSimulator = config.asBoolean("sdclient.simulator.enabled", false);
        if (useProdBasClient) {
            log.info("Using production Desigo API client");
            String apiUrl = config.asString("sd.api.url", "http://<localhost>:<port>");
            apiUri = URI.create(apiUrl);
            String username = config.asString("sd.api.username", "admin");
            String password = config.asString("sd.api.password", "admin");
            desigoApiClient = new DesigoApiClientRest(apiUri, username, password, buildStubNotificationListener());

        } else if (useBasSimulator) {
            log.info("Using Desigo BAS client simulator");
            desigoApiClient = new SdClientSimulator();
        }
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
    public List<IngestionService> createIngestionServices(ObservationListener observationListener, NotificationListener notificationListener) {
        if (observationListener == null || notificationListener == null) {
            throw new RealEstateException("Missing parameters: observationListener {} or notificationListener {}.");
        }
        List<IngestionService> ingestionServices = new ArrayList<>();
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }

        boolean useSimulators = config.asBoolean("ingestionServices.simulator.enabled", false);
        if (useSimulators) {
            ingestionServices.add(new DesigoPresentValueIngestionServiceSimulator());
            log.info("Added DesigoPresentValueIngestionServiceSimulator");
            ingestionServices.add(new DesigoTrendsIngestionServiceSimulator());
            log.info("Added DesigoTrendsIngestionServiceSimulator");
        } else {
            if (desigoApiClient == null) {
                throw new RealEstateException("Missing DesigoApiClient. Please call initialize() first.");
            }
            PresentValueIngestionService presentValueService = createPresentValueIngestionService(desigoApiClient, observationListener, notificationListener);
            ingestionServices.add(presentValueService);
            log.info("Added DesigoPresentValueIngestionService");
            TrendsLastUpdatedService trendsLastUpdatedService = createTrendsLastUpdatedService(config);
            TrendsIngestionService trendsIngestionService = createTrendsIngestionService(desigoApiClient, trendsLastUpdatedService, observationListener, notificationListener);
            ingestionServices.add(trendsIngestionService);
            log.info("Added DesigoTrendsIngestionService");
        }
        return ingestionServices;
    }

    protected TrendsLastUpdatedService createTrendsLastUpdatedService(PluginConfig config) {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        TrendsLastUpdatedService lastUpdatedService = null;
        boolean useAzure = config.asBoolean("lastUpdated.azure", false);
        boolean useCsv = config.asBoolean("lastUpdated.csv", false);

        if (useAzure) {
            TrendsLastUpdatedRepository repository = new TrendsLastUpdatedRepository();
            AzureTableClient lastUpdatedTableClient = createLastUpdatedTableClient(config);
            AzureTableClient lastFailedTableClient = createLastFailedTableClient(config);
            lastUpdatedService = new  AzureTrendsLastUpdatedService(config, repository,lastUpdatedTableClient, lastFailedTableClient);
            log.info("Created TrendsLastUpdatedService: {} with trendsLastUpdatedRepository {}lastUpdatedTableClient " +
                    "{} and lastFailedTableClient {}", lastUpdatedService, repository, lastUpdatedTableClient, lastFailedTableClient);
        } else if (useCsv) {
            throw new RealEstateException("CSV not implemented yet");
        } else {
            lastUpdatedService = new InMemoryTrendsLastUpdatedService();
        }
        log.info("Created TrendsLastUpdatedService: {}. Config useAzure {}, useCsv {}", lastUpdatedService.getClass(), useAzure, useCsv);
        return lastUpdatedService;
    }



    protected PresentValueIngestionService createPresentValueIngestionService(BasClient desigoApiClient, ObservationListener observationListener, NotificationListener notificationListener) {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        PresentValueIngestionService presentValueService = new DesigoPresentValueIngestionService(config, observationListener, notificationListener, desigoApiClient);
        log.info("Created PresentValueIngestionService: {}", presentValueService);
        return presentValueService;
    }
    protected TrendsIngestionService createTrendsIngestionService(BasClient desigoApiClient, TrendsLastUpdatedService trendsLastUpdatedService, ObservationListener observationListener, NotificationListener notificationListener) {
        if (config == null) {
            throw new RealEstateException("Missing configuration. Please call initialize() first.");
        }
        TrendsIngestionService trendsIngestionService = new DesigoTrendsIngestionService(config,  observationListener, notificationListener, desigoApiClient, trendsLastUpdatedService);
        log.info("Created TrendsIngestionService: {} with desigoApiClient {} and trendsLastUpdatedService {}", trendsIngestionService, desigoApiClient, trendsLastUpdatedService);
        return trendsIngestionService;
    }

    @Override
    public List<DistributionService> createDistributionServices() {
        log.warn("DistributionServices not implemented for Desigo");
        return null;
    }

    private NotificationListener buildStubNotificationListener() {
        return new NotificationListener() {
            @Override
            public void sendWarning(String s, String s1, String s2) {
                log.info("**Need** real Notification listener {}, {}, {}", s, s1, s2);
            }

            @Override
            public void sendAlarm(String s, String s1, String s2) {
                log.info("**Need** real Notification listener {}, {}, {}", s, s1, s2);
            }

            @Override
            public void clearService(String s, String s1) {
                log.info("**Need** real Notification listener {}, {}, {}", s, s1);
            }

            @Override
            public void setHealthy(String s, String s1) {
                log.info("**Need** real Notification listener {}, {}, {}", s, s1);
            }

            @Override
            public void setUnhealthy(String s, String s1, String s2) {
                log.info("**Need** real Notification listener {}, {}, {}", s, s1, s2);
            }

            @Override
            public void addError(String s, String s1, String s2) {
                log.info("**Need** real Notification listener {}, {}, {}", s, s1, s2);
            }
        };
    }
}
