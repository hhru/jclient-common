package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;

import java.util.Map;
import java.util.Set;

import static ru.hh.jclient.common.HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER;

public abstract class UpstreamManager {
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;

  public abstract Upstream getUpstream(String host);

  public Upstream getDynamicUpstream(String host, String key) {
    return getUpstream(host);
  };

  public abstract Map<String, Upstream> getUpstreams();

  public abstract void updateUpstream(String name, String configString);

  public abstract Set<Monitoring> getMonitoring();

  public double getTimeoutMultiplier() {
    return timeoutMultiplier;
  }

  public void setTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
  }
}
