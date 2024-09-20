package no.cantara.realestate.plugin.desigo.automationserver;

import no.cantara.realestate.automationserver.BasClient;
import no.cantara.realestate.observations.PresentValue;
import no.cantara.realestate.plugin.desigo.sensor.DesigoSensorMappingSimulator;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.security.LogonFailedException;
import no.cantara.realestate.security.UserToken;
import no.cantara.realestate.sensors.MappedSensorId;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.slf4j.Logger;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static no.cantara.realestate.utils.UrlEncoder.urlEncode;
import static org.slf4j.LoggerFactory.getLogger;

public class SdClientSimulator implements BasClient {

    private static final Logger log = getLogger(SdClientSimulator.class);
    private Map<String, List<DesigoTrendSample>> simulatedSDApiData = new ConcurrentHashMap();
    boolean scheduled_simulator_started = true;
    private final int SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS = 3;
    private Set<String> simulatedTrendIds = new HashSet<>();
    private long numberOfTrendSamplesReceived = 0;
    private boolean isHealty = false;


    public SdClientSimulator() {
        log.info("SD Rest API Simulator started");
        scheduled_simulator_started = false;
        initializeMapAndStartSimulation();
    }

    /**
     * Add TrendId's used for simulation here, or in DesigoSensorMappingSimulator
     */
    private void initializeMapAndStartSimulation() {
        List<MappedSensorId> mappedSensors = DesigoSensorMappingSimulator.getSimulatedSensors();
        for (MappedSensorId mappedSensor : mappedSensors) {
            DesigoSensorId sensorId = (DesigoSensorId) mappedSensor.getSensorId();
            simulatedTrendIds.add(sensorId.getTrendId());
        }
        startScheduledSimulationOfTrendValues();
    }

    @Override
    public PresentValue findPresentValue(SensorId sensorId) throws URISyntaxException, LogonFailedException {
        DesigoSensorId desigoSensorId = (DesigoSensorId) sensorId;
        DesigoPresentValue presentValue =  new DesigoPresentValue();
        Value value = new Value();
        value.setValue(521);
        value.setSampleDate(Instant.now());
        presentValue.setValue(value);
        presentValue.setObjectId(desigoSensorId.getDesigoId());
        presentValue.setPropertyId(desigoSensorId.getDesigoPropertyId());
        return presentValue;
    }

    @Override
    public Set<DesigoTrendSample> findTrendSamplesByDate(String trendId, int take, int skip, Instant onAndAfterDateTime) throws URISyntaxException {
        Set<DesigoTrendSample> trendSamples = new HashSet<>();
        List<DesigoTrendSample> trendTimeSamples = simulatedSDApiData.get(trendId);
        int count = 0;
        if (trendTimeSamples != null) {
            for (DesigoTrendSample trendTimeSample : trendTimeSamples) {
                if (trendTimeSample.getObservedAt().isAfter(onAndAfterDateTime)) {
                    trendSamples.add(trendTimeSample);
                    count++;
                    if (count > take) {
                        break;
                    }
                }
            }
        }
        log.info("findTrendSamples returned:{} trendSamples", trendSamples.size());
        return trendSamples;
    }


    @Override
    public void logon() throws LogonFailedException {
        return;
    }

    @Override
    public UserToken refreshToken() throws LogonFailedException {
        return null;
    }

    String encodeAndPrefix(String trendId) {
        if (trendId != null) {
            return urlEncode(trendId);
        } else {
            return null;
        }
    }

    private void startScheduledSimulationOfTrendValues() {
        if (!scheduled_simulator_started) {
            scheduled_simulator_started = true;
            ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
            log.info("Initializing TrendValue simulator");

            Runnable task1 = () -> {
                try {
                    simulateSensorReadings();
                } catch (Exception e) {
                    log.info("Exception trying to run simulated generation of trendvalues");
                }
            };

            // init Delay = 5, repeat the task every 60 second
            ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(task1, 5, SECONDS_BETWEEN_SCHEDULED_IMPORT_RUNS, TimeUnit.SECONDS);
        }
    }

    private void addTrendIdToSimulation(String trendId) {
        simulatedTrendIds.add(trendId);
    }

    public void simulateSensorReadings() {
//        log.info("starting SD Sensor simulator run");

        for (String trendid : simulatedTrendIds) {
            //  We generate trensValues for 20% for each run
//            log.trace("Running SD Sensor simulator for: {}", trendid);
            Integer randomValue = ThreadLocalRandom.current().nextInt(100);
            //  We generate trensValues for 20% for each run
            if (randomValue < 90) {
                addSimulatedSDTrendSample(trendid);
            }
        }
    }

    @Override
    public Integer subscribePresentValueChange(String subscriptionId, String objectId) throws URISyntaxException, LogonFailedException {
        if (subscriptionId == null || objectId == null) {
            return 400;
        }

        if (subscriptionId.isEmpty() || objectId.isEmpty()) {
            return 404;
        }
        return 202;
    }

    protected void addSimulatedSDTrendSample(String trendId) {
        DesigoTrendSample trendSample = new DesigoTrendSample();
        trendSample.setTrendId(trendId);
        Instant observedAt = Instant.now();
        trendSample.setTimestamp(observedAt.toString());
        Integer randomValue = ThreadLocalRandom.current().nextInt(50);
        trendSample.setValue(randomValue);
        List<DesigoTrendSample> trendSimulations = simulatedSDApiData.get(trendId);
        if (trendSimulations == null) {
            trendSimulations = new ArrayList<>();
        }
        trendSimulations.add(trendSample);
        simulatedSDApiData.put(trendId, trendSimulations);

//        log.info("   - added trendSample for {} - new size: {}", trendId, tsMap.size());
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public String getName() {
        return "SdClientSimulator";

    }

    @Override
    public boolean isHealthy() {
        return isHealty;
    }

    @Override
    public long getNumberOfTrendSamplesReceived() {
        return numberOfTrendSamplesReceived;
    }

    @Override
    public UserToken getUserToken() {
        return new UserToken();
    }

    public void openConnection(String username, String password, NotificationListener notificationListener) {
        isHealty = true;
    }
}
