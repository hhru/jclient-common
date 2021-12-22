package ru.hh.jclient.common.balancing.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class Profile {
  @JsonProperty("max_tries")
  private Integer maxTries;
  @JsonProperty("max_timeout_tries")
  private Integer maxTimeoutTries;
  @JsonProperty("connect_timeout_sec")
  private Float connectTimeoutSec;
  @JsonProperty("request_timeout_sec")
  private Float requestTimeoutSec;
  @JsonProperty("slow_start_interval_sec")
  private Integer slowStartIntervalSec;
  @JsonProperty("retry_policy")
  private Map<Integer, RetryPolicyConfig> retryPolicy;
  @JsonProperty("session_required")
  private Boolean isSessionRequired;

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

  public Float getConnectTimeoutSec() {
    return connectTimeoutSec;
  }

  public Profile setConnectTimeoutSec(Float connectTimeoutSec) {
    this.connectTimeoutSec = connectTimeoutSec;
    return this;
  }

  public Float getRequestTimeoutSec() {
    return requestTimeoutSec;
  }

  public Profile setRequestTimeoutSec(Float requestTimeoutSec) {
    this.requestTimeoutSec = requestTimeoutSec;
    return this;
  }

  public Integer getSlowStartIntervalSec() {
    return slowStartIntervalSec;
  }

  public Profile setSlowStartIntervalSec(Integer slowStartIntervalSec) {
    this.slowStartIntervalSec = slowStartIntervalSec;
    return this;
  }

  public Boolean isSessionRequired() {
    return this.isSessionRequired;
  }

  public Profile setSessionRequired(boolean sessionRequired) {
    this.isSessionRequired = sessionRequired;
    return this;
  }

  @Override
  public String toString() {
    return "Profile{" +
        ", maxTries=" + maxTries +
        ", maxTimeoutTries=" + maxTimeoutTries +
        ", connectTimeoutMs=" + connectTimeoutSec +
        ", requestTimeoutMs=" + requestTimeoutSec +
        ", slowStartIntervalSec=" + slowStartIntervalSec +
        ", isSessionRequired=" + isSessionRequired +
        ", retryPolicy=" + retryPolicy +
        '}';
  }
}
