package ru.hh.jclient.common.balancing.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class Profile {
  @JsonProperty("max_tries")
  private Integer maxTries;
  @JsonProperty("max_timeout_tries")
  private Integer maxTimeoutTries;
  @JsonProperty("connect_timeout_sec")
  private Float connectTimeoutMs;
  @JsonProperty("request_timeout_sec")
  private Float requestTimeoutMs;
  @JsonProperty("slow_start_interval_sec")
  private Integer slowStartIntervalSec;
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

  public Integer getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  public Profile setMaxTimeoutTries(Integer maxTimeoutTries) {
    this.maxTimeoutTries = maxTimeoutTries;
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

  public Integer getSlowStartIntervalSec() {
    return slowStartIntervalSec;
  }

  public Profile setSlowStartIntervalSec(Integer slowStartIntervalSec) {
    this.slowStartIntervalSec = slowStartIntervalSec;
    return this;
  }

  @Override
  public String toString() {
    return "Profile{" +
        ", maxTries=" + maxTries +
        ", maxTimeoutTries=" + maxTimeoutTries +
        ", connectTimeoutMs=" + connectTimeoutMs +
        ", requestTimeoutMs=" + requestTimeoutMs +
        ", slowStartIntervalSec=" + slowStartIntervalSec +
        ", retryPolicy=" + retryPolicy +
        '}';
  }
}
