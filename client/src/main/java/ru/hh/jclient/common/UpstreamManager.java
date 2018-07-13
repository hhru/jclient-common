package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;

import java.util.Map;

import static ru.hh.jclient.common.HttpClientConfig.DEFAULT_TIMEOUT_MULTIPLIER;

public abstract class UpstreamManager {
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;

  public abstract Upstream getUpstream(String host);

  public abstract Map<String, Upstream> getUpstreams();

  public abstract void updateUpstream(String name, String configString);

  public abstract Monitoring getMonitoring();

  public double getTimeoutMultiplier() {
    return timeoutMultiplier;
  }

  public void setTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
  }
}
