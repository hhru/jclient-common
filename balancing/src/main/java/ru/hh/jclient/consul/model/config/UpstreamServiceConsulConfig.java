package ru.hh.jclient.consul.model.config;

import ru.hh.consul.option.ConsistencyMode;
import ru.hh.jclient.consul.model.ConsistencyModeConfig;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import static ru.hh.jclient.consul.PropertyKeys.ALLOW_CROSS_DC_KEY;
import static ru.hh.jclient.consul.PropertyKeys.CONSISTENCY_MODE_KEY;
import static ru.hh.jclient.consul.PropertyKeys.DATACENTER_KEY;
import static ru.hh.jclient.consul.PropertyKeys.DC_LIST_KEY;
import static ru.hh.jclient.consul.PropertyKeys.HEALTHY_ONLY_KEY;
import static ru.hh.jclient.consul.PropertyKeys.NODE_NAME_KEY;
import static ru.hh.jclient.consul.PropertyKeys.SELF_NODE_FILTERING_KEY;
import static ru.hh.jclient.consul.PropertyKeys.SYNC_UPDATE_KEY;
import static ru.hh.jclient.consul.PropertyKeys.UPSTREAMS_KEY;
import static ru.hh.jclient.consul.PropertyKeys.WATCH_SECONDS_KEY;

public class UpstreamServiceConsulConfig {
  private List<String> upstreams;
  private boolean allowCrossDC;
  private boolean healthPassing;
  private int watchSeconds;
  private String currentDC;
  private String currentNode;
  private ConsistencyMode consistencyMode;
  private List<String> datacenterList;
  private boolean selfNodeFilteringEnabled;
  private boolean syncInit = true;

  private static UpstreamServiceConsulConfig fromPropertiesWithDefaults(Properties props) {
    var upstreams = Optional.ofNullable(props.getProperty(UPSTREAMS_KEY))
      .filter(Predicate.not(String::isBlank))
      .map(separatedList -> List.of(separatedList.split("[,\\s]+")))
      .orElseGet(List::of);
    boolean allowCrossDC = Optional.ofNullable(props.getProperty(ALLOW_CROSS_DC_KEY)).map(Boolean::parseBoolean).orElse(false);
    boolean healthyOnly = Optional.ofNullable(props.getProperty(HEALTHY_ONLY_KEY)).map(Boolean::parseBoolean).orElse(false);
    boolean selfNodeFiltering = Optional.ofNullable(props.getProperty(SELF_NODE_FILTERING_KEY)).map(Boolean::parseBoolean).orElse(false);
    var watchSeconds = Optional.ofNullable(props.getProperty(WATCH_SECONDS_KEY)).stream().mapToInt(Integer::parseInt).findFirst().orElse(10);
    var dcList = Optional.ofNullable(props.getProperty(DC_LIST_KEY))
      .filter(Predicate.not(String::isBlank))
      .map(separatedDcList -> List.of(separatedDcList.split("[,\\s]+")))
      .orElseGet(List::of);
    var consistencyMode = Optional.ofNullable(props.getProperty(CONSISTENCY_MODE_KEY))
      .map(ConsistencyModeConfig::valueOf)
      .orElse(ConsistencyModeConfig.DEFAULT);
    boolean syncUpdate = Optional.ofNullable(props.getProperty(SYNC_UPDATE_KEY)).map(Boolean::parseBoolean).orElse(true);
    return new UpstreamServiceConsulConfig()
      .setUpstreams(upstreams)
      .setAllowCrossDC(allowCrossDC)
      .setHealthPassing(healthyOnly)
      .setSelfNodeFilteringEnabled(selfNodeFiltering)
      .setWatchSeconds(watchSeconds)
      .setDatacenterList(dcList)
      .setCurrentDC(props.getProperty(DATACENTER_KEY))
      .setCurrentNode(props.getProperty(NODE_NAME_KEY))
      .setConsistencyMode(consistencyMode.consistencyMode)
      .setSyncInit(syncUpdate);
  }

  public List<String> getUpstreams() {
    return upstreams;
  }

  public UpstreamServiceConsulConfig setUpstreams(List<String> upstreams) {
    this.upstreams = upstreams;
    return this;
  }

  public boolean isAllowCrossDC() {
    return allowCrossDC;
  }

  public UpstreamServiceConsulConfig setAllowCrossDC(boolean allowCrossDC) {
    this.allowCrossDC = allowCrossDC;
    return this;
  }

  public boolean isHealthPassing() {
    return healthPassing;
  }

  public UpstreamServiceConsulConfig setHealthPassing(boolean healthPassing) {
    this.healthPassing = healthPassing;
    return this;
  }

  public boolean isSelfNodeFilteringEnabled() {
    return selfNodeFilteringEnabled;
  }

  public UpstreamServiceConsulConfig setSelfNodeFilteringEnabled(boolean selfNodeFilteringEnabled) {
    this.selfNodeFilteringEnabled = selfNodeFilteringEnabled;
    return this;
  }

  public int getWatchSeconds() {
    return watchSeconds;
  }

  public UpstreamServiceConsulConfig setWatchSeconds(int watchSeconds) {
    this.watchSeconds = watchSeconds;
    return this;
  }

  public String getCurrentDC() {
    return currentDC;
  }

  public UpstreamServiceConsulConfig setCurrentDC(String currentDC) {
    this.currentDC = currentDC;
    return this;
  }

  public String getCurrentNode() {
    return currentNode;
  }

  public UpstreamServiceConsulConfig setCurrentNode(String currentNode) {
    this.currentNode = currentNode;
    return this;
  }

  public ConsistencyMode getConsistencyMode() {
    return consistencyMode;
  }

  public UpstreamServiceConsulConfig setConsistencyMode(ConsistencyMode consistencyMode) {
    this.consistencyMode = consistencyMode;
    return this;
  }

  public List<String> getDatacenterList() {
    return datacenterList;
  }

  public UpstreamServiceConsulConfig setDatacenterList(List<String> datacenterList) {
    this.datacenterList = datacenterList;
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
  public UpstreamServiceConsulConfig setSyncInitTimeoutMillis(Integer syncInitTimeoutMillis) {
    return this;
  }

  public boolean isSyncInit() {
    return syncInit;
  }

  public UpstreamServiceConsulConfig setSyncInit(boolean syncInit) {
    this.syncInit = syncInit;
    return this;
  }
}
