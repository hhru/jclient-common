package ru.hh.jclient.consul.model.config;

import ru.hh.consul.option.ConsistencyMode;

public class UpstreamConfigServiceConsulConfig {
  private int watchSeconds;
  private ConsistencyMode consistencyMode;
  private boolean syncUpdate = true;

  public int getWatchSeconds() {
    return watchSeconds;
  }

  public UpstreamConfigServiceConsulConfig setWatchSeconds(int watchSeconds) {
    this.watchSeconds = watchSeconds;
    return this;
  }

  public ConsistencyMode getConsistencyMode() {
    return consistencyMode;
  }

  public UpstreamConfigServiceConsulConfig setConsistencyMode(ConsistencyMode consistencyMode) {
    this.consistencyMode = consistencyMode;
    return this;
  }

  public boolean isSyncUpdate() {
    return syncUpdate;
  }

  public UpstreamConfigServiceConsulConfig setSyncUpdate(boolean syncUpdate) {
    this.syncUpdate = syncUpdate;
    return this;
  }

  /**
   * value is no longer used
   * @return always 0
   */
  @Deprecated(forRemoval = true)
  public int getSyncInitTimeoutMillis() {
    return 0;
  }

  /**
   * value is no longer used
   */
  @Deprecated(forRemoval = true)
  public UpstreamConfigServiceConsulConfig setSyncInitTimeoutMillis(int syncInitTimeoutMillis) {
    return this;
  }
}
