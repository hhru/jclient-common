package ru.hh.jclient.consul;

import ru.hh.jclient.consul.model.ApplicationConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TestUpstreamConfigService implements UpstreamConfigService {

    private final Map<String, ApplicationConfig> configs;

    public TestUpstreamConfigService() {
        this.configs = new HashMap<>();
    }

    public void setServers(String application, ApplicationConfig applicationConfig) {
        this.configs.put(application, applicationConfig);
    }

    public void clearServers() {
        this.configs.clear();
    }

    @Override
    public void setupListener(Consumer<String> callback) {

    }

    @Override
    public ApplicationConfig getUpstreamConfig(String application) {
        return configs.get(application);
    }
}
