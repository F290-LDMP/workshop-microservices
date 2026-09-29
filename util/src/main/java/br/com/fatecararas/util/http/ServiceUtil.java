package br.com.fatecararas.util.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil implements ApplicationListener<WebServerInitializedEvent> {
    private final ApplicationContext applicationContext;
    private volatile Integer serverPort;
    private volatile String serverIp;

    public ServiceUtil(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (event.getApplicationContext() != applicationContext) {
            return;
        }
        serverPort = event.getWebServer().getPort();
        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            serverIp = "unknown";
        }
    }

    public Integer getServerPort() { return serverPort; }
    public String getServerIp() { return serverIp; }

    public String getServerAddress() {
        if (serverPort == null) {
            throw new IllegalStateException("Servidor HTTP ainda não iniciado");
        }
        String host = serverIp.contains(":") ? "[" + serverIp + "]" : serverIp;
        return "http://" + host + ":" + serverPort;
    }
}
