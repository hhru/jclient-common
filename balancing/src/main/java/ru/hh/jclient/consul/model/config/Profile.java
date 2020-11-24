package ru.hh.jclient.consul.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Profile {
  private String name;
  @JsonProperty("max_tries")
  private Integer maxTries;
  @JsonProperty("max_fails")
  private Integer maxFails;
  @JsonProperty("max_timeout_tries")
  private Integer maxTimeoutTries;
  @JsonProperty("fail_timeout_sec")
  private Float failTimeoutMs;
  @JsonProperty("connect_timeout_sec")
  private Float connectTimeoutMs;
  @JsonProperty("request_timeout_sec")
  private Float requestTimeoutMs;
  @JsonProperty("retry_policy")
  private Map<Integer, RetryPolicyConfig> retryPolicy;

  public Map<Integer, RetryPolicyConfig> getRetryPolicy() {
    return retryPolicy;
  }

  public Profile setRetryPolicy(Map<Integer, RetryPolicyConfig> retryPolicy) {
    this.retryPolicy = retryPolicy;
    return this;
  }

  public Integer getMaxTries() {
    return maxTries;
  }

  public Profile setMaxTries(Integer maxTries) {
    this.maxTries = maxTries;
    return this;
  }

  public Integer getMaxFails() {
    return maxFails;
  }

  public Profile setMaxFails(Integer maxFails) {
    this.maxFails = maxFails;
    return this;
  }

  public Integer getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  public Profile setMaxTimeoutTries(Integer maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
    return this;
  }

  public Float getFailTimeoutMs() {
    return failTimeoutMs;
  }

  public Profile setFailTimeoutMs(Float failTimeoutMs) {
    this.failTimeoutMs = failTimeoutMs;
    return this;
  }

  public Float getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public Profile setConnectTimeoutMs(Float connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
    return this;
  }

  public Float getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public Profile setRequestTimeoutMs(Float requestTimeoutMs) {
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
