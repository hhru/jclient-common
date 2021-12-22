package ru.hh.jclient.common;

public class RequestContext {
  public static final RequestContext EMPTY_CONTEXT = new RequestContext(null,  null);

  public final String upstreamName;
  public final String datacenter;
  public final boolean isSessionRequired;

  private RequestDebug contextDebug;

  public RequestContext(String upstreamName, String datacenter) {
    this(upstreamName, datacenter, false);
  }

  public RequestContext(String upstreamName, String datacenter, boolean isSessionRequired) {
    this.upstreamName = upstreamName;
    this.datacenter = datacenter;
    this.isSessionRequired = isSessionRequired;
  }

  public RequestDebug getContextDebug() {
    return contextDebug;
  }

  public void setContextDebug(RequestDebug contextDebug) {
    this.contextDebug = contextDebug;
  }

  @Override
  public String toString() {
    return "upstream: " + upstreamName + ", dc: " + datacenter + ", isSessionRequired: " + isSessionRequired;
  }
}
