package ru.hh.consul.client.model;

public final class ConsulUpstreamConfig {
  private int maxTries;
  private int maxFails;
  private int maxTimeoutTries;

  private int failTimeoutMs;
  private int connectTimeoutMs;
  private int requestTimeoutMs;
  private boolean allowCrossDCRequests;

  private RetryPolicy retryPolicy = new RetryPolicy();

  public int getMaxTries() {
    return maxTries;
  }

  public ConsulUpstreamConfig setMaxTries(int maxTries) {
    this.maxTries = maxTries;
    return this;
  }

  public int getMaxFails() {
    return maxFails;
  }

  public ConsulUpstreamConfig setMaxFails(int maxFails) {
    this.maxFails = maxFails;
    return this;
  }

  public int getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  public ConsulUpstreamConfig setMaxTimeoutTries(int maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
    return this;
  }

  public int getFailTimeoutMs() {
    return failTimeoutMs;
  }

  public ConsulUpstreamConfig setFailTimeoutMs(int failTimeoutMs) {
    this.failTimeoutMs = failTimeoutMs;
    return this;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public ConsulUpstreamConfig setConnectTimeoutMs(int connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
    return this;
  }

  public int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public ConsulUpstreamConfig setRequestTimeoutMs(int requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
    return this;
  }

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  public ConsulUpstreamConfig setRetryPolicy(RetryPolicy retryPolicy) {
    this.retryPolicy = retryPolicy;
    return this;
  }
}
