package ru.hh.jclient.common.balancing;

final class ServerEntry {
  private final int index;
  private final String address;
  private final String hostName;
  private final String datacenter;


  ServerEntry(int index, String address, String hostName, String datacenter) {
    this.index = index;
    this.address = address;
    this.hostName = hostName;
    this.datacenter = datacenter;
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

  public String getHostName() {
    return hostName;
  }
}
