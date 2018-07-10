package ru.hh.jclient.common;

public class DebugConfig {

  private final int connectTimeout;

  private final int requestTimeout;

  private final int maxRedirects;

  private DebugConfig(int connectTimeout, int requestTimeout, int maxRedirects) {
    this.connectTimeout = connectTimeout;
    this.requestTimeout = requestTimeout;
    this.maxRedirects = maxRedirects;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public int getMaxRedirects() {
    return maxRedirects;
  }

  public static DebugConfigBuilder builder() {
    return new DebugConfigBuilder();
  }

  public static class DebugConfigBuilder {

    private int connectTimeout;

    private int requestTimeout;

    private int maxRedirects;

    public DebugConfigBuilder connectTimeout(int connectionTimeout) {
      this.connectTimeout = connectionTimeout;
      return this;
    }

    public DebugConfigBuilder requestTimeout(int requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    public DebugConfigBuilder maxRedirects(int maxRedirects) {
      this.maxRedirects = maxRedirects;
      return this;
    }

    public DebugConfig build() {
      return new DebugConfig(connectTimeout, requestTimeout, maxRedirects);
    }

  }
}
