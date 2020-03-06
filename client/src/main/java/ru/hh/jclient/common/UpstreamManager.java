package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;
import ru.hh.jclient.common.balancing.UpstreamProfileSelector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static ru.hh.jclient.common.HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER;

public abstract class UpstreamManager {
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;

  public Upstream getUpstream(String serviceName) {
    return getUpstream(serviceName, null);
  }

  public abstract Upstream getUpstream(String serviceName, @Nullable String profile);

  @Nonnull
  protected abstract UpstreamProfileSelector getProfileSelector(HttpClientContext ctx);

  public abstract void updateUpstream(@Nonnull String upstreamName, String configString);

  public abstract Set<Monitoring> getMonitoring();

  public double getTimeoutMultiplier() {
    return timeoutMultiplier;
  }

  public void setTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
  }
}
