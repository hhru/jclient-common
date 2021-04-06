package ru.hh.jclient.consul.model.config;

import ru.hh.consul.option.ConsistencyMode;
import ru.hh.jclient.consul.model.ConsistencyModeConfig;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import static ru.hh.jclient.consul.PropertyKeys.CONSISTENCY_MODE_KEY;
import static ru.hh.jclient.consul.PropertyKeys.UPSTREAMS_KEY;
import static ru.hh.jclient.consul.PropertyKeys.SYNC_UPDATE_KEY;
import static ru.hh.jclient.consul.PropertyKeys.WATCH_SECONDS_KEY;

public class UpstreamConfigServiceConsulConfig {
  private List<String> upstreams;
  private int watchSeconds;
  private ConsistencyMode consistencyMode;
  private boolean syncUpdate = true;

  public static UpstreamConfigServiceConsulConfig fromPropertiesWithDefaults(Properties props) {
    var upstreams = Optional.ofNullable(props.getProperty(UPSTREAMS_KEY))
      .filter(Predicate.not(String::isBlank))
      .map(separatedList -> List.of(separatedList.split("[,\\s]+")))
      .orElseGet(List::of);
    var watchSeconds = Optional.ofNullable(props.getProperty(WATCH_SECONDS_KEY)).stream()
      .mapToInt(Integer::parseInt)
      .findFirst()
      .orElse(10);
    var consistencyMode = Optional.ofNullable(props.getProperty(CONSISTENCY_MODE_KEY))
      .map(ConsistencyModeConfig::valueOf)
      .orElse(ConsistencyModeConfig.DEFAULT);
    boolean syncUpdate = Optional.ofNullable(props.getProperty(SYNC_UPDATE_KEY)).map(Boolean::parseBoolean).orElse(true);
    return new UpstreamConfigServiceConsulConfig()
      .setUpstreams(upstreams)
      .setWatchSeconds(watchSeconds)
      .setConsistencyMode(consistencyMode.consistencyMode)
      .setSyncUpdate(syncUpdate);
  }

  public static UpstreamConfigServiceConsulConfig copyOf(UpstreamConfigServiceConsulConfig config) {
    return new UpstreamConfigServiceConsulConfig().setUpstreams(config.upstreams)
      .setWatchSeconds(config.watchSeconds)
      .setConsistencyMode(config.getConsistencyMode())
      .setSyncUpdate(config.isSyncUpdate());
  }

  public List<String> getUpstreams() {
    return upstreams;
  }

  public UpstreamConfigServiceConsulConfig setUpstreams(List<String> upstreams) {
    this.upstreams = upstreams;
    return this;
  }

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
}
