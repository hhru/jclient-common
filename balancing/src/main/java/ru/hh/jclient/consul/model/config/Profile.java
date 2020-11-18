package ru.hh.jclient.consul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Profile {
  private String name;
  @JsonProperty("max_tries")
  private int maxTries;
  @JsonProperty("max_fails")
  private int maxFails;
  @JsonProperty("max_timeout_tries")
  private int maxTimeoutTries;
  @JsonProperty("fail_timeout_sec")
  private int failTimeoutMs;
  @JsonProperty("connect_timeout_sec")
  private int connectTimeoutMs;
  @JsonProperty("request_timeout_sec")
  private int requestTimeoutMs;
  @JsonProperty("retry_policy")
  private Map<Integer, RetryPolicyConfig> retryPolicy;

  public Map<Integer, RetryPolicyConfig> getRetryPolicy() {
    return retryPolicy;
  }

  public Profile setRetryPolicy(Map<Integer, RetryPolicyConfig> retryPolicy) {
    this.retryPolicy = retryPolicy;
    return this;
  }

  public int getMaxTries() {
    return maxTries;
  }

  public Profile setMaxTries(int maxTries) {
    this.maxTries = maxTries;
    return this;
  }

  public int getMaxFails() {
    return maxFails;
  }

  public Profile setMaxFails(int maxFails) {
    this.maxFails = maxFails;
    return this;
  }

  public int getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  public Profile setMaxTimeoutTries(int maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
    return this;
  }

  public int getFailTimeoutMs() {
    return failTimeoutMs;
  }

  public Profile setFailTimeoutMs(int failTimeoutMs) {
    this.failTimeoutMs = failTimeoutMs;
    return this;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public Profile setConnectTimeoutMs(int connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
    return this;
  }

  public int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public Profile setRequestTimeoutMs(int requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
    return this;
  }

  public String getName() {
    return name;
  }

  public Profile setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String toString() {
    return "Profile{" +
        "name='" + name + '\'' +
        ", maxTries=" + maxTries +
        ", maxFails=" + maxFails +
        '}';
  }
}
