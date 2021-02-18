package ru.hh.jclient.consul.model.config;

import ru.hh.consul.option.ConsistencyMode;

public class UpstreamConfigServiceConsulConfig {
  private int watchSeconds;
  private ConsistencyMode consistencyMode;
  private boolean syncUpdate = true;
  private int syncInitTimeoutMillis;

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

  public int getSyncInitTimeoutMillis() {
    return syncInitTimeoutMillis;
  }

  public UpstreamConfigServiceConsulConfig setSyncInitTimeoutMillis(int syncInitTimeoutMillis) {
    this.syncInitTimeoutMillis = syncInitTimeoutMillis;
    return this;
  }
}
