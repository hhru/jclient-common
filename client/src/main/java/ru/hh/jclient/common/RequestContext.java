package ru.hh.jclient.common;

public class RequestContext {
  public static final RequestContext EMPTY_CONTEXT = new RequestContext(null,  null, null);

  private final String upstreamName;
  private final String destinationDatacenter;
  private final String destinationHost;
  private final boolean isSessionRequired;

  public RequestContext(String upstreamName, String destinationDatacenter, String destinationHost) {
    this(upstreamName, destinationDatacenter, destinationHost, false);
  }

  public RequestContext(String upstreamName, String destinationDatacenter, String destinationHost, boolean isSessionRequired) {
    this.upstreamName = upstreamName;
    this.destinationDatacenter = destinationDatacenter;
    this.destinationHost = destinationHost;
    this.isSessionRequired = isSessionRequired;
  }

  public String getUpstreamName() {
    return upstreamName;
  }

  public String getDestinationDatacenter() {
    return destinationDatacenter;
  }

  public String getDestinationHost() {
    return destinationHost;
  }

  public boolean isSessionRequired() {
    return isSessionRequired;
  }

  @Override
  public String toString() {
    return "RequestContext{" +
           "upstreamName='" + upstreamName + '\'' +
           ", destinationDatacenter='" + destinationDatacenter + '\'' +
           ", destinationHost='" + destinationHost + '\'' +
           ", isSessionRequired=" + isSessionRequired +
           '}';
  }
}
