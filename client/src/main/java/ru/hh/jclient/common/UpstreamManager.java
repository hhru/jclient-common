package ru.hh.jclient.common;

import javax.annotation.Nonnull;
import java.util.Set;

import static ru.hh.jclient.common.HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER;

public abstract class UpstreamManager {
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;

  public abstract void updateUpstream(@Nonnull String upstreamName, String configString);

  public abstract Set<Monitoring> getMonitoring();

  public double getTimeoutMultiplier() {
    return timeoutMultiplier;
  }

  public void setTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
  }
}
