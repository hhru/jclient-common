package ru.hh.consul.client.model;

public class ConsulUpstream {
  private String serviceName;
  private String datacenter;
  private String address;
  private int weight;

  public String getServiceName() {
    return serviceName;
  }

  public ConsulUpstream setServiceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public ConsulUpstream setDatacenter(String datacenter) {
    this.datacenter = datacenter;
    return this;
  }

  public String getAddress() {
    return address;
  }

  public ConsulUpstream setAddress(String address) {
    this.address = address;
    return this;
  }

  public int getWeight() {
    return weight;
  }

  public ConsulUpstream setWeight(int weight) {
    this.weight = weight;
    return this;
  }
}
