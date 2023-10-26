package no.cantara.realestate.plugin.desigo.automationserver;

import no.cantara.realestate.plugin.desigo.MockServerSetup;
import no.cantara.realestate.plugin.desigo.notifications.NotificationListenerStub;
import no.cantara.realestate.security.LogonFailedException;
import org.slf4j.Logger;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

public class DesigoApiClientManualTest {
    private static final Logger log = getLogger(DesigoApiClientManualTest.class);

    public static void main(String[] args) throws LogonFailedException {
        String apiUrl = MockServerSetup.API_URL;
        MockServerSetup.clearAndSetLoginMock();
        MockServerSetup.clearAndSetSensorMockData("8648f9cf-c135-5471-9906-9b3861e0b5ab");
        //MockServerSetup.clearAndSetSensorMockData("208540b1-ab8a-566a-8a41-8b4cee515baf");
        DesigoApiClientRest apiClient = new DesigoApiClientRest(URI.create(apiUrl));
        apiClient.openConnection(null, null, new NotificationListenerStub());
        apiClient.logon("jane-doe","strongPassword");
        assertTrue(apiClient.isHealthy());
    }
}
