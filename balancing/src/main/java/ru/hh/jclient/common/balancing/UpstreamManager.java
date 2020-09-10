package ru.hh.jclient.common.balancing;

import static ru.hh.jclient.common.HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER;
import ru.hh.jclient.common.Monitoring;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class UpstreamManager {
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;

  public abstract List<Server> getServersForService(String upstreamName);

  public abstract void updateUpstream(@Nonnull String upstreamName);

  public abstract Upstream getUpstream(String serviceName, @Nullable String profile);

  public Upstream getUpstream(String serviceName) {
    return getUpstream(serviceName, null);
  }

  abstract Map<String, UpstreamGroup> getUpstreams();

  public abstract Set<Monitoring> getMonitoring();

  public double getTimeoutMultiplier() {
    return timeoutMultiplier;
  }

  public void setTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
  }
}
