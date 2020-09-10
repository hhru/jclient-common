package ru.hh.jclient.common;

public class RequestContext {
  public static final RequestContext EMPTY_CONTEXT = new RequestContext(null,  null);

  public final String upstreamName;
  public final String datacenter;

  private RequestDebug contextDebug;

  public RequestContext(String upstreamName, String datacenter) {
    this.upstreamName = upstreamName;
    this.datacenter = datacenter;
  }

  public RequestDebug getContextDebug() {
    return contextDebug;
  }

  public void setContextDebug(RequestDebug contextDebug) {
    this.contextDebug = contextDebug;
  }

  @Override
  public String toString() {
    return "upstream: " + upstreamName + ", dc: " + datacenter;
  }
}
