package se.magnus.microservices.recommendation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceAddress implements ApplicationListener<WebServerInitializedEvent> {

    private volatile Integer serverPort;
    private volatile String serverIp;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        serverPort = event.getWebServer().getPort();
        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            serverIp = "unknown";
        }
    }

    public String getAddress() {
        return serverIp == null || serverPort == null ? "unknown" : serverIp + ":" + serverPort;
    }
}
