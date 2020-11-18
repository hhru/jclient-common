package ru.hh.jclient.consul.model.config;

import java.util.Map;

public class Host {
  public Map<String, Profile> profiles;

  @Override
  public String toString() {
    return "Host{" +
        "profiles=" + profiles +
        '}';
  }
}
