package ru.hh.jclient.consul.model.config;

import com.orbitz.consul.option.ConsistencyMode;

import java.util.List;

public class UpstreamServiceConsulConfig {
  private boolean allowCrossDC;
  private boolean healthPassing;
  private int watchSeconds;
  private String currentDC;
  private String currentNode;
  private ConsistencyMode consistencyMode;
  private List<String> datacenterList;

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
}
