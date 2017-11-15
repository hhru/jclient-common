package ru.hh.jclient.common.balancing;

public class ServerEntry {
  private final int index;
  private final String address;

  ServerEntry(int index, String address) {
    this.index = index;
    this.address = address;
  }

  public int getIndex() {
    return index;
  }

  public String getAddress() {
    return address;
  }
}
