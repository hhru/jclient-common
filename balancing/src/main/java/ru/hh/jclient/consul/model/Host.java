package ru.hh.jclient.consul.model;

import java.util.Map;

public class Host {
  private Map<String, Profile> profiles;

  public Map<String, Profile> getProfiles() {
    return profiles;
  }

  public Host setProfiles(Map<String, Profile> profiles) {
    this.profiles = profiles;
    return this;
  }

  @Override
  public String toString() {
    return "Host{" +
        "profiles=" + profiles +
        '}';
  }
}
