package ru.hh.jclient.common.balancing;

public interface JClientInfrastructureConfig {
    String getServiceName();
    String getCurrentDC();
    String getCurrentNodeName();
}
