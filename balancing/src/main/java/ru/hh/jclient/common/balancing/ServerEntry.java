package ru.hh.jclient.common.balancing;

import java.util.Map;

final class ServerEntry {
  private final int index;
  private final String address;
  private final String datacenter;
  private final Map<String, String> meta;


  ServerEntry(int index, String address, String datacenter, Map<String, String> meta) {
    this.index = index;
    this.address = address;
    this.datacenter = datacenter;
    this.meta = meta;
  }

  public int getIndex() {
    return index;
  }

  public String getAddress() {
    return address;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public Map<String, String> getMeta() {
    return meta;
  }
}
