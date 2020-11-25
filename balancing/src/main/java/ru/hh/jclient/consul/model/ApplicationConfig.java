package ru.hh.jclient.consul.model;

import java.util.Map;

public class ApplicationConfig {
  private Map<String, Host> hosts;

  public Map<String, Host> getHosts() {
    return hosts;
  }

  public ApplicationConfig setHosts(Map<String, Host> hosts) {
    this.hosts = hosts;
    return this;
  }

  @Override
  public String toString() {
    return "ApplicationConfig{" +
        "hosts=" + hosts +
        '}';
  }
}