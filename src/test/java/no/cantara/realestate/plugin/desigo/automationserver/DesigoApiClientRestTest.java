package no.cantara.realestate.plugin.desigo.automationserver;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DesigoApiClientRestTest {


    @Test
    void instantWithSecondsAccuracyTest() {
        assertEquals("2020-10-10T10:00:00Z", DesigoApiClientRest.instantWithSecondsAccuracy(Instant.parse("2020-10-10T10:00:00.000Z")));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10:00:00.000+00:00"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10:00:00.000Z"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10:00:00Z"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10:00:00.000"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10:00:00"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10:00"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10T10"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11-10"));
//        assertEquals("2020-11-10T10:00:00Z", desigoApiClientRest.instantWithSecondsAccuracy("2020-11"));
    }
}