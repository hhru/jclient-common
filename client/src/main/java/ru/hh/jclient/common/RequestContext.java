package ru.hh.jclient.common;

public class RequestContext {
  public static final RequestContext EMPTY_CONTEXT = new RequestContext(null, null, null);

  public final String upstreamName;
  public final String rack;
  public final String datacenter;

  public RequestContext(String upstreamName, String rack, String datacenter) {
    this.upstreamName = upstreamName;
    this.rack = rack;
    this.datacenter = datacenter;
  }
}
