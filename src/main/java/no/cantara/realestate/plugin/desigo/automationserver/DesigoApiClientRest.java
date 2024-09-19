package no.cantara.realestate.plugin.desigo.automationserver;


import com.google.common.net.HttpHeaders;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import no.cantara.realestate.automationserver.BasClient;
import no.cantara.realestate.json.RealEstateObjectMapper;
import no.cantara.realestate.observations.TrendSample;
import no.cantara.realestate.plugin.desigo.DesigoCloudConnectorException;
import no.cantara.realestate.plugin.desigo.notifications.NotificationListenerStub;
import no.cantara.realestate.plugins.notifications.NotificationListener;
import no.cantara.realestate.security.LogonFailedException;
import no.cantara.realestate.security.UserToken;
import no.cantara.realestate.sensors.SensorId;
import no.cantara.realestate.sensors.desigo.DesigoSensorId;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.cantara.realestate.plugin.desigo.DesigoRealEstatePluginFactory.PLUGIN_ID;
import static no.cantara.realestate.utils.StringUtils.hasValue;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoApiClientRest implements BasClient {
    private static final Logger log = getLogger(DesigoApiClientRest.class);
    private static final String DESIGO_SUBSCRIBE_HEADER = "DESIGO-SUBSCRIBE";
    private final URI apiUri;

    public static final String DESIGO_API = "DesigoApiClient";
    public static final String HOST_UNREACHABLE = "HOST_UNREACHABLE";
    public static final String LOGON_FAILED = "Logon to Desigo Api Failed";
    public static final String SERVICE_FAILED = "Desigo Api is failing.";
    public static final String UNKNOWN_HOST = "UNKNOWN_HOST";

    private static final String LATEST_BY_DATE = "SampleDateDescending";
    private NotificationListener notificationListener;
    private DesigoUserToken userToken = null;
    private long numberOfTrendSamplesReceived = 0;
    private boolean isHealthy = true;

    private String username;
    private String password;

    final Tracer tracer;
    final Meter meter;

    public DesigoApiClientRest(URI apiUri) {
        tracer = GlobalOpenTelemetry.getTracer("no.cantara.realestate");
        meter = GlobalOpenTelemetry.getMeter("no.cantara.realestate");
        this.apiUri = apiUri;
    }

    public DesigoApiClientRest(URI apiUri, String username, String password, NotificationListener notificationListener) {
        tracer = GlobalOpenTelemetry.getTracer("no.cantara.realestate");
        meter = GlobalOpenTelemetry.getMeter("no.cantara.realestate");
        this.apiUri = apiUri;
        this.username = username;
        this.password = password;
        this.notificationListener = notificationListener;
    }

    public void openConnection(String username, String password, NotificationListener notificationListener) throws LogonFailedException {
        this.username = username;
        this.password = password;
        this.notificationListener = notificationListener;
        logon(username, password);
    }

    @Override
    public Set<TrendSample> findTrendSamplesByDate(String trendId, int take, int skip, Instant onAndAfterDateTime) throws URISyntaxException, LogonFailedException {
        Span span = tracer.spanBuilder("findTrendSamplesByDate").setSpanKind(SpanKind.CLIENT).startSpan();
        log.trace("findTrendSamplesByDate. trendId: {}. From date: {}. Take: {}. Skip: {}",
                trendId, onAndAfterDateTime, take, skip);
        String bearerToken = findAccessToken();
        URI samplesUri = new URI(apiUri + "trendseries/" + trendId);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = null;
        List<DesigoTrendSample> trendSamples = new ArrayList<>();
        try (Scope ignored = span.makeCurrent()) {

            String startTime = instantWithSecondsAccuracy(onAndAfterDateTime);
            log.trace("validate From date: onAndAfterDateTime: {}. startTime: {} should be simmilar", onAndAfterDateTime, startTime);
            int page = 1;
            int pageSize = 1000;
            String endTime = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS).toString();
//            log.trace("findTrendSamplesByDate. trendId: {}. From date: {}. To date: {}. Page: {}. PageSize: {}. Take: {}. Skip: {}",
//                    trendId, startTime, endTime, page, pageSize, take, skip);
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("from", startTime));
            nvps.add(new BasicNameValuePair("to", endTime));

            URI uri = new URIBuilder(samplesUri)
                    .addParameters(nvps)
                    .build();
            request = new HttpGet(uri);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            log.trace("Find TrendSamples for TrendId: {} from uri: {} From date: {}, with parameters: {} ", trendId, uri, startTime, nvps);
            CloseableHttpResponse response = httpClient.execute(request);

            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        DesigoTrendSampleResult trendSampleResult = TrendSamplesMapper.mapFromJson(body);
                        log.trace("Found: {} trends from trendId: {}", trendSampleResult.getTotal(), trendId);
                        trendSamples = trendSampleResult.getSeriesWithObjectAndPropertyId();
                        if (trendSamples != null) {
                            for (DesigoTrendSample trendSample : trendSamples) {
                                trendSample.setTrendId(trendId);
                                log.trace("imported trendSample: {}", trendSample);
                            }
                        }

                    }
                } else {
                    span.addEvent("warning-response-code-not-200");
                }
            } catch (Exception e) {
                setUnhealthy();
                DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to fetch trendsamples for objectId " + trendId
                        + ", after date " + onAndAfterDateTime + ". Reason: " + e.getMessage(), e);
                span.recordException(de);
                throw de;
            }
        } catch (Exception e) {
            setUnhealthy();
            DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to fetch trendsamples for objectId " + trendId
                    + ", after date " + onAndAfterDateTime + ". Reason: " + e.getMessage(), e);
            span.recordException(de);
            throw de;
        }

        /*
        String startTime = onAndAfterDateTime.toString();
        //FIXME make dynamic
        int page=1;
        int pageSize=1000;
        String endTime = Instant.now().plusSeconds(60).toString();



//        DesigoTrendSampleResult trendSampleResult = trendSampleService.findTrendSamplesByDate("Bearer " + bearerToken, prefixedUrlEncodedTrendId, pageSize, page, startTime, endTime);
        log.trace("findTrendSamplesByDate. trendId: {}. From date: {}. To date: {}. Page: {}. PageSize: {}. Take: {}. Skip: {}",
                objectId, onAndAfterDateTime, endTime, page, pageSize, take, skip);
        String trendSamplesJson = trendSampleService.findTrendSamplesByDateJson("Bearer " + bearerToken, prefixedUrlEncodedTrendId, pageSize, page, startTime, endTime);


        DesigoTrendSampleResult trendSampleResult = TrendSamplesMapper.mapFromJson(trendSamplesJson);
        log.trace("Found: {} trends from trendId: {}", trendSampleResult.getTotal(), objectId);
        List<DesigoTrendSample> trendSamples = trendSampleResult.getItems();
        if (trendSamples != null) {
            for (DesigoTrendSample trendSample : trendSamples) {
                trendSample.setTrendId(objectId);
                log.trace("imported trendSample: {}", trendSample);
            }
        }

         */
        log.trace("findTrendSamplesByDate. trendId: {}. From date: {}. Take: {}. Skip: {}. Found: {}",
                trendId, onAndAfterDateTime, take, skip, trendSamples.size());
        isHealthy = true;
        meter.histogramBuilder("OTEL.AzureMonitor.Desigo.TrendSamples").build().record(trendSamples.size());
        span.end();
        return new HashSet<>(trendSamples);
    }

    protected static String instantWithSecondsAccuracy(Instant onAndAfterDateTime) {
        return onAndAfterDateTime.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    @Override
    public DesigoPresentValue findPresentValue(SensorId sensorId) throws URISyntaxException, LogonFailedException {
        Span span = tracer.spanBuilder("findPresentValue").setSpanKind(SpanKind.CLIENT).startSpan();
        DesigoPresentValue presentValue = null;
        DesigoSensorId desigoSensorId = (DesigoSensorId) sensorId;
        String objectOrPropertyId = desigoSensorId.getDesigoId() + "." + desigoSensorId.getDesigoPropertyId();
        String bearerToken = findAccessToken();
        URI presentValueUri = new URI(apiUri + "values/" + objectOrPropertyId);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = null;
        List<DesigoTrendSample> trendSamples = new ArrayList<>();
        try (Scope ignored = span.makeCurrent()) {
            log.trace("findPresentValue. objectOrPropertyId: {}.", objectOrPropertyId);
            request = new HttpGet(presentValueUri);
            request.addHeader(HttpHeaders.ACCEPT, "application/json");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        if (body != null && body.startsWith("[")) {

                            DesigoPresentValue[] presentValues = PresentValueMapper.mapFromJsonArray(body);
                            if (presentValues != null) {
                                log.trace("Found: {} presentValues from objectOrPropertyId: {}", presentValues.length, objectOrPropertyId);
                                presentValue = presentValues[0];
                                meter.counterBuilder("OTEL.AzureMonitor.Desigo.PresentValues").build().add(1);
                            }
                        } else {
                            presentValue = PresentValueMapper.mapFromJson(body);
                        }
                    }
                }
            } catch (Exception e) {
                setUnhealthy();
                DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to fetch presentValue for sensorId " + desigoSensorId + ". Reason: " + e.getMessage(), e);
                span.recordException(de);
                throw de;
            }
        } catch (Exception e) {
            setUnhealthy();
            DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to fetch presentValue for sensorId " + desigoSensorId + ". Reason: " + e.getMessage(), e);
            span.recordException(de);
            throw de;
        }
        span.end();
        return presentValue;
    }

    @Override
    public Integer subscribePresentValueChange(String subscriptionId, String objectId) throws URISyntaxException, LogonFailedException {
        Span span = tracer.spanBuilder("subscribePresentValueChange").setSpanKind(SpanKind.CLIENT).startSpan();
        Integer statusCode = null;

        String bearerToken = findAccessToken();
        URI subscribeUri = new URI(apiUri + "objects/" + objectId + "/attributes/presentValue?includeSchema=false");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = null;
        try (Scope ignored = span.makeCurrent()) {
            request = new HttpGet(subscribeUri);
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            request.addHeader(DESIGO_SUBSCRIBE_HEADER, subscriptionId);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                HttpEntity entity = response.getEntity();
                statusCode = response.getCode();
                if (statusCode == 202) {
                    log.trace("Subscribing ok for objectId: {}", objectId);
                    meter.counterBuilder("OTEL.AzureMonitor.Desigo.PresentValues.Subscriptions").build().add(1);
                } else {
                    String body = "";
                    if (entity != null) {
                        body = EntityUtils.toString(entity);
                    }
                    log.trace("Could not subscribe to subscription {} for objectId {} using URL: {}. Status: {}. Body text: {}", subscriptionId, objectId, subscribeUri, statusCode, body);
                    span.addEvent("info-subscribe-failed");
                }
            } catch (Exception e) {
                setUnhealthy();
                DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to subscribe to objectId " + objectId
                        + ". Reason: " + e.getMessage(), e);
                span.recordException(de);
                throw de;
            }
        } catch (Exception e) {
            setUnhealthy();
            DesigoCloudConnectorException de = new DesigoCloudConnectorException("Failed to subscribe to objectId " + objectId
                    + ". Reason: " + e.getMessage(), e);
            span.recordException(de);
            throw de;
        }
        span.end();
        return statusCode;

    }


    boolean tokenNeedRefresh() {
        if (userToken == null) {
            return true;
        }
        boolean tokenNeedRefresh = userToken.tokenNeedRefresh();
        return tokenNeedRefresh;
    }

    public DesigoUserToken refreshToken() throws LogonFailedException {
        DesigoUserToken refreshedUserToken = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String refreshTokenUrl = apiUri + "refreshToken";
        HttpGet request = null;
        String truncatedAccessToken = null;
        try {

            request = new HttpGet(refreshTokenUrl);
            String accessToken = userToken.getAccessToken();
            if (accessToken != null && accessToken.length() > 11) {
                truncatedAccessToken = accessToken.substring(0, 10) + "...";
            } else {
                truncatedAccessToken = accessToken;
            }
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            log.trace("Try to logon to Desigo at uri: {}", refreshTokenUrl);
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        userToken = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(body, DesigoUserToken.class);
                        log.trace("Converted to userToken: {}", userToken);
                        refreshedUserToken = userToken;
                        log.debug("Logged on to Desigo. Refreshed accessToken. Expires: {}", userToken.getExpires().toString());
                        setHealthy();
                    }
                } else {
                    String msg = "Failed to refresh userToken to Desigo at uri: " + request.getRequestUri() +
                            ". accessToken: " + truncatedAccessToken +
                            ". ResponseCode: " + httpCode +
                            ". ReasonPhrase: " + response.getReasonPhrase();
                    LogonFailedException logonFailedException = new LogonFailedException(msg);
                    log.warn("Failed to refresh accessToken on Desigo. Reason {}", logonFailedException.getMessage());
                    setUnhealthy();
                    notificationListener.addError(PLUGIN_ID, DESIGO_API, "Failed to refresh accessToken on Desigo. Reason: " + logonFailedException.getMessage());
                    throw logonFailedException;
                }

            } finally {
                response.close();
            }
        } catch (IOException e) {
            notificationListener.sendAlarm(PLUGIN_ID, DESIGO_API, HOST_UNREACHABLE);
            String msg = "Failed to refresh accessToken on Desigo at uri: " + refreshTokenUrl + ", with accessToken: " + truncatedAccessToken;
            LogonFailedException logonFailedException = new LogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            notificationListener.addError(PLUGIN_ID, DESIGO_API, msg + " Reason: " + logonFailedException.getMessage());
            throw logonFailedException;
        } catch (ParseException e) {
            notificationListener.sendWarning(PLUGIN_ID, DESIGO_API, "Parsing of AccessToken information failed.");
            String msg = "Failed to refresh accessToken on Desigo at uri: " + refreshTokenUrl + ", with accessToken: " + truncatedAccessToken +
                    ". Failure parsing the response.";
            LogonFailedException logonFailedException = new LogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            notificationListener.addError(PLUGIN_ID, DESIGO_API, msg + " Reason: " + logonFailedException.getMessage());
            throw logonFailedException;
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //Do nothing
            }
        }
        return refreshedUserToken;
    }

    @Override
    public void logon() throws LogonFailedException {
        logon(username, password);
    }

    protected void logon(String username, String password) throws LogonFailedException {
        if (!hasValue(username)) {
            log.warn("Trying to logon with username null. Please call openConnection(username, password) first.");
            throw new LogonFailedException("Username is null. Please call openConnection(username, password) first.");
        }
        log.trace("Logon: {}", username);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String loginUri = apiUri + "token";
        HttpPost request = null;
        try {
            request = new HttpPost(loginUri);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            request.addHeader(HttpHeaders.ACCEPT, "application/json");
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("username", username));
            nvps.add(new BasicNameValuePair("password", password));
            nvps.add(new BasicNameValuePair("grant_type", "password"));
            HttpEntity bodyEntity = new UrlEncodedFormEntity(nvps);
            request.setEntity(bodyEntity);

            CloseableHttpResponse response = httpClient.execute(request);
            try {
                int httpCode = response.getCode();
                if (httpCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String body = EntityUtils.toString(entity);
                        log.trace("Received body: {}", body);
                        userToken = RealEstateObjectMapper.getInstance().getObjectMapper().readValue(body, DesigoUserToken.class);
                        log.trace("Converted to userToken: {}", userToken);
                        setHealthy();
                        if (notificationListener != null) {
                            notificationListener.clearService(PLUGIN_ID, DESIGO_API);
                        }
                    }
                } else {
                    String requestHeaders = "";
                    for (NameValuePair nvp : request.getHeaders()) {
                        requestHeaders += nvp.getName() + "=" + nvp.getValue() + ", ";
                    }
                    String responseHeaders = "";
                    for (NameValuePair nvp : response.getHeaders()) {
                        responseHeaders += nvp.getName() + "=" + nvp.getValue() + ", ";
                    }
                    String body = "";
                    if (response.getEntity() != null) {
                        body = EntityUtils.toString(response.getEntity());
                    }
                    String msg = "Failed to logon to Desigo at uri: " + loginUri +
                            ". Username: " + username +
                            ". RequestHeaders: " + requestHeaders +
                            ". RequestMethod: " + request.getMethod() +
                            ". ResponseCode: " + httpCode +
                            ". ReasonPhrase: " + response.getReasonPhrase() +
                            ". ResponseHeaders: " + responseHeaders +
                            ". Body text: " + body;
                    LogonFailedException logonFailedException = new LogonFailedException(msg);
                    log.warn("Failed to logon to Desigo. Reason {}", logonFailedException.getMessage());
                    setUnhealthy();
                    if (notificationListener != null) {
                        notificationListener.sendWarning(PLUGIN_ID, DESIGO_API, LOGON_FAILED);
                    }
                    log.warn("Failed to logon to Desigo. Reason: " + logonFailedException.getMessage());
                    throw logonFailedException;
                }

            } finally {
                response.close();
            }
        } catch (IOException e) {
            if (notificationListener != null) {
                notificationListener.sendAlarm(PLUGIN_ID, DESIGO_API, HOST_UNREACHABLE);
            }
            String msg = "Failed to logon to Desigo at uri: " + loginUri + ", with username: " + username;
            LogonFailedException logonFailedException = new LogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            throw logonFailedException;
        } catch (ParseException e) {
            if (notificationListener != null) {
                notificationListener.sendWarning(PLUGIN_ID, DESIGO_API, "Parsing of login information failed.");
            }
            String msg = "Failed to logon to Desigo at uri: " + loginUri + ", with username: " + username +
                    ". Failure parsing the response.";
            LogonFailedException logonFailedException = new LogonFailedException(msg, e);
            log.warn(msg);
            setUnhealthy();
            throw logonFailedException;
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                //Do nothing
            }
        }
    }

    @Override
    public boolean isLoggedIn() {
        return userToken != null;
    }

    @Override
    public String getName() {
        return "DesigoApiClientRest";
    }

    void setHealthy() {
        if (!this.isHealthy) {
            this.isHealthy = true;
            log.trace("Desigo is Healthy");
            if (notificationListener != null) {
                notificationListener.setHealthy(PLUGIN_ID, DESIGO_API);
            }
        }
    }

    void setUnhealthy() {
        if (this.isHealthy) {
            log.warn("Desigo is Unhealthy");
            this.isHealthy = false;
            if (notificationListener != null) {
                notificationListener.setUnhealthy(PLUGIN_ID, DESIGO_API, "See log for details");
            }
        }
    }


    public boolean isHealthy() {
        return isHealthy;
    }

    @Override
    public long getNumberOfTrendSamplesReceived() {
        return numberOfTrendSamplesReceived;
    }

    synchronized void addNumberOfTrendSamplesReceived() {
        if (numberOfTrendSamplesReceived < Long.MAX_VALUE) {
            numberOfTrendSamplesReceived++;
        } else {
            numberOfTrendSamplesReceived = 1;
        }
    }

    public UserToken getUserToken() {
        return userToken;
    }

    private String findAccessToken() throws LogonFailedException {
        try {
            String accessToken = null;
            if (userToken == null || tokenNeedRefresh()) {
                logon();
            }
            if (userToken != null) {
                accessToken = userToken.getAccessToken();
            } else {
                notificationListener.clearService(PLUGIN_ID, DESIGO_API);
            }

            return accessToken;
        } catch (LogonFailedException e) {
            notificationListener.sendAlarm(PLUGIN_ID, DESIGO_API, LOGON_FAILED);
            isHealthy = false;
            throw e;
        }
    }

    public static void main(String[] args) throws URISyntaxException, LogonFailedException {

        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");

        String apiUrl = args[0]; //getConfigValue("sd.api.url");
        String trendId = args[1]; //getConfigValue("trend.id");
        String userName = args[2];
        String password = args[3];
        URI apiUri = new URI(apiUrl);

        DesigoApiClientRest apiClient = new DesigoApiClientRest(apiUri);
        apiClient.openConnection(userName, password, new NotificationListenerStub());
        String bearerToken = apiClient.findAccessToken();
        Set<TrendSample> trends = apiClient.findTrendSamplesByDate(trendId, 10, 0, Instant.now().minus(30, ChronoUnit.DAYS));
        for (TrendSample trend : trends) {
            if (trend != null) {
                log.info("Trend id={}, value={}, valid={}", trend.getTrendId(), trend.getValue(), trend.isValid());
            } else {
                log.info("Trend is null");
            }
        }
    }

}
