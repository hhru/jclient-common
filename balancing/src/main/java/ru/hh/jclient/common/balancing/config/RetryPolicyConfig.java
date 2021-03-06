package ru.hh.jclient.common.balancing.config;

public class RetryPolicyConfig {
  private boolean idempotent;

  public boolean isIdempotent() {
    return idempotent;
  }

  public RetryPolicyConfig setIdempotent(boolean idempotent) {
    this.idempotent = idempotent;
    return this;
  }

  @Override
  public String toString() {
    return "RetryPolicyConfig{" +
        "idempotent=" + idempotent +
        '}';
  }
}
