package ru.hh.jclient.common.balancing;

final class ServerEntry {
  private final int index;
  private final String address;
  private final String rack;
  private final String datacenter;

  ServerEntry(int index, String address, String rack, String datacenter) {
    this.index = index;
    this.address = address;
    this.rack = rack;
    this.datacenter = datacenter;
  }

  public int getIndex() {
    return index;
  }

  public String getAddress() {
    return address;
  }

  public String getRack() {
    return rack;
  }

  public String getDatacenter() {
    return datacenter;
  }
}
