package se.magnus.util.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil implements ApplicationListener<WebServerInitializedEvent> {

    private Integer serverPort;
    private String serverIp;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.serverPort = event.getWebServer().getPort();
        try {
            this.serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            this.serverIp = "unknown";
        }
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public String getServerIp() {
        return serverIp;
    }

    public String getServiceAddress() {
        return serverIp == null || serverPort == null ? "unknown" : serverIp + ":" + serverPort;
    }
}
