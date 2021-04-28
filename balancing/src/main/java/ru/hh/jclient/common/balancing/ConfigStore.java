package ru.hh.jclient.common.balancing;

public interface ConfigStore {
    UpstreamConfigs getUpstreamConfig(String upstream);
    void updateConfig(String upstream, UpstreamConfigs upstreamConfigs);
}
