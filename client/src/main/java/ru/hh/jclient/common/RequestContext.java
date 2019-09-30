package ru.hh.jclient.common;

public class RequestContext {
  public static final RequestContext EMPTY_CONTEXT = new RequestContext(null, null, null);

  public final String upstreamName;
  public final String rack;
  public final String datacenter;

  private RequestDebug contextDebug;

  public RequestContext(String upstreamName, String rack, String datacenter) {
    this.upstreamName = upstreamName;
    this.rack = rack;
    this.datacenter = datacenter;
  }

  public RequestDebug getContextDebug() {
    return contextDebug;
  }

  public void setContextDebug(RequestDebug contextDebug) {
    this.contextDebug = contextDebug;
  }
}
