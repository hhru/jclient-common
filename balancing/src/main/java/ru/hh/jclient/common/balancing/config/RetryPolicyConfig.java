package ru.hh.jclient.common.balancing.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetryPolicyConfig {
  @JsonProperty("retry_non_idempotent")
  private boolean retryNonIdempotent;

  public boolean isRetryNonIdempotent() {
    return retryNonIdempotent;
  }

  public RetryPolicyConfig setRetryNonIdempotent(boolean retryNonIdempotent) {
    this.retryNonIdempotent = retryNonIdempotent;
    return this;
  }

  @Override
  public String toString() {
    return "RetryPolicyConfig{" +
        "retryNonIdempotent=" + retryNonIdempotent +
        '}';
  }
}
