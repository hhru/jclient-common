package ru.hh.jclient.consul.model;

import com.orbitz.consul.option.ConsistencyMode;

public enum ConsistencyModeConfig {
  DEFAULT(ConsistencyMode.DEFAULT),
  STALE(ConsistencyMode.STALE),
  CONSISTENT(ConsistencyMode.CONSISTENT);

  public final ConsistencyMode consistencyMode;

  ConsistencyModeConfig(ConsistencyMode consistencyMode) {
    this.consistencyMode = consistencyMode;
  }
}
