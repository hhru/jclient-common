package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.balancing.config.ApplicationConfig;

public interface ConfigStore {
    ApplicationConfig getUpstreamConfig(String upstream);
    void updateConfig(String upstream, ApplicationConfig applicationConfig);
}
