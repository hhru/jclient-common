package ru.hh.jclient.consul.model.config;

public interface JClientInfrastructureConfig {
    String getServiceName();
    String getCurrentDC();
    String getCurrentNodeName();
}
