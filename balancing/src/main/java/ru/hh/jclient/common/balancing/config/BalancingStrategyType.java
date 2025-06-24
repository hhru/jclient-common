package ru.hh.jclient.common.balancing.config;

import jakarta.annotation.Nullable;
import java.util.Arrays;

public enum BalancingStrategyType {
  WEIGHTED,
  ADAPTIVE,
  ;

  public String getPublicName() {
    return name().toLowerCase();
  }

  @Nullable
  public static BalancingStrategyType tryParseFromString(@Nullable String value) {
    if (value == null) {
      return null;
    }

    return Arrays.stream(values())
        .filter(type -> type.getPublicName().equals(value))
        .findFirst()
        .orElse(null);
  }
}
