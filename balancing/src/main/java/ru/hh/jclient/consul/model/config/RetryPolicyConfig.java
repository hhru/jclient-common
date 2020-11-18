package ru.hh.jclient.consul.model.config;

public class RetryPolicyConfig {
  private boolean idempotent;

  public boolean isIdempotent() {
    return idempotent;
  }

  public RetryPolicyConfig setIdempotent(boolean idempotent) {
    this.idempotent = idempotent;
    return this;
  }
}
