package no.cantara.realestate.plugin.desigo.notifications;


import no.cantara.realestate.plugins.notifications.NotificationListener;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class NotificationListenerStub implements NotificationListener {
    private static final Logger log = getLogger(NotificationListenerStub.class);

    @Override
    public void sendWarning(String pluginId, String service, String warningMessage) {
        log.warn("Warning received from Plugin {} with Service {} and Message {}", pluginId, service, warningMessage);
    }

    @Override
    public void sendAlarm(String pluginId, String service, String warningMessage) {
        log.error("Alarm received from Plugin {} with Service {} and Message {}", pluginId, service, warningMessage);
    }

    @Override
    public void clearService(String pluginId, String service) {
        log.info("Service {} is back to normal, from Plugin {}", service, pluginId);
    }

    @Override
    public void setHealthy(String s, String s1) {

    }

    @Override
    public void setUnhealthy(String s, String s1, String s2) {

    }

    @Override
    public void addError(String s, String s1, String s2) {

    }
}
